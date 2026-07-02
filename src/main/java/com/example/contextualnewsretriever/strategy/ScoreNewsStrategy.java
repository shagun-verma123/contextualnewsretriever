package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.llm.LlmService;
import com.example.contextualnewsretriever.mapper.ArticleMapper;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ScoreNewsStrategy implements NewsRetrievalStrategy {

    private final NewsArticleRepository repository;
    private final LlmService llmService;

    @Qualifier("llmExecutor")
    private final TaskExecutor llmExecutor;

    @Override
    public NewsIntent supportedIntent() {
        return NewsIntent.SCORE;
    }

    @Override
    public List<ArticleResponse> retrieve(RetrievalCriteria criteria) {
        double minScore = criteria.getMinScore() != null ? criteria.getMinScore() : 0.0;
        List<ArticleResponse> articles = repository.findByRelevanceScoreGreaterThanEqualOrderByRelevanceScoreDesc(
                        minScore, PageRequest.of(0, criteria.getLimit()))
                .stream()
                .map(ArticleMapper::toResponse)
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
}
