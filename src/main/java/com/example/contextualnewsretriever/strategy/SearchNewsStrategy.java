package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.exception.InvalidRequestException;
import com.example.contextualnewsretriever.llm.LlmService;
import com.example.contextualnewsretriever.mapper.ArticleMapper;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.example.contextualnewsretriever.util.TextMatchUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Ranks by relevanceScore*0.6 + textMatchScore*0.4. The dataset is small (~2000 rows), so
 * scoring in memory against the full table is simpler and more explainable than trying to
 * push token-overlap scoring into SQL, at the cost of not scaling past a single-node dataset
 * this size - a fine trade-off for this assignment (noted in the README).
 */
@Component
@RequiredArgsConstructor
public class SearchNewsStrategy implements NewsRetrievalStrategy {

    private static final double RELEVANCE_WEIGHT = 0.6;
    private static final double TEXT_MATCH_WEIGHT = 0.4;

    private final NewsArticleRepository repository;
    private final LlmService llmService;

    @Qualifier("llmExecutor")
    private final TaskExecutor llmExecutor;

    @Override
    public NewsIntent supportedIntent() {
        return NewsIntent.SEARCH;
    }

    @Override
    public List<ArticleResponse> retrieve(RetrievalCriteria criteria) {
        if (criteria.getSearchQuery() == null || criteria.getSearchQuery().isBlank()) {
            throw new InvalidRequestException("query is required for the SEARCH intent");
        }
        String query = criteria.getSearchQuery();
        List<ArticleResponse> articles = repository.findAll().stream()
                .map(article -> scored(article, query))
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(criteria.getLimit())
                .map(Scored::response)
                .toList();

        List<CompletableFuture<Void>> futures = articles.stream()
                .map(article -> CompletableFuture.runAsync(() -> {
                    String summary = llmService.summarizeArticle(
                            article.getTitle(),
                            article.getDescription()
                    );
                    article.setLlmSummary(summary);
                }, llmExecutor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return articles;
    }

    private Scored scored(NewsArticle article, String query) {
        double textMatch = Math.max(
                TextMatchUtil.tokenOverlapScore(query, article.getTitle()),
                TextMatchUtil.tokenOverlapScore(query, article.getDescription()));
        double relevance = article.getRelevanceScore() != null ? article.getRelevanceScore() : 0.0;
        double combined = relevance * RELEVANCE_WEIGHT + textMatch * TEXT_MATCH_WEIGHT;
        return new Scored(combined, ArticleMapper.toResponse(article));
    }

    private record Scored(double score, ArticleResponse response) {
    }
}
