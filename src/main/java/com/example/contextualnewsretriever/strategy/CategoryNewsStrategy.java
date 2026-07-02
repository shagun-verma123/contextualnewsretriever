package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.exception.InvalidRequestException;
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
public class CategoryNewsStrategy implements NewsRetrievalStrategy {

    private final NewsArticleRepository repository;
    @Qualifier("llmExecutor")
    private final TaskExecutor llmExecutor;
    private final LlmService llmService;

    @Override
    public NewsIntent supportedIntent() {
        return NewsIntent.CATEGORY;
    }


    @Override
    public List<ArticleResponse> retrieve(RetrievalCriteria criteria) {
        if (criteria.getCategory() == null || criteria.getCategory().isBlank()) {
            throw new InvalidRequestException("category is required for the CATEGORY intent");
        }
        List<ArticleResponse> articles =  repository.findByCategory(criteria.getCategory(), PageRequest.of(0, criteria.getLimit()))
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
