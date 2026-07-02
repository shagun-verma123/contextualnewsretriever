package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.NewsResponse;
import com.example.contextualnewsretriever.dto.SmartQueryRequest;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.exception.InvalidRequestException;
import com.example.contextualnewsretriever.llm.LlmService;
import com.example.contextualnewsretriever.llm.QueryUnderstandingResponse;
import com.example.contextualnewsretriever.strategy.NewsStrategyFactory;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges free-text queries to the same NewsRetrievalStrategy implementations used by the
 * direct REST endpoints. LLM output is never trusted blindly: an unrecognized intent string
 * falls back to SEARCH, and a NEARBY intent without coordinates fails fast with a 400 rather
 * than silently returning an unrelated result set.
 */
@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private final LlmService llmService;
    private final NewsStrategyFactory strategyFactory;

    @Qualifier("llmExecutor")
    private final TaskExecutor llmExecutor;

    public NewsResponse handleSmartQuery(SmartQueryRequest request) {
        QueryUnderstandingResponse understanding = llmService.understandQuery(request.getQuery());
        NewsIntent intent = NewsIntent.fromStringOrDefault(understanding.getIntent(), NewsIntent.SEARCH);
        Map<String, String> entities = understanding.getEntities() != null ? understanding.getEntities() : Map.of();

        if (intent == NewsIntent.NEARBY && (request.getLatitude() == null || request.getLongitude() == null)) {
            throw new InvalidRequestException("latitude and longitude are required in the request body for a nearby query");
        }

        RetrievalCriteria criteria = RetrievalCriteria.builder()
                .intent(intent)
                .category(entities.get("category"))
                .source(entities.get("source"))
                .searchQuery(understanding.getSearchQuery() != null ? understanding.getSearchQuery() : request.getQuery())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .radiusKm(request.getRadiusKm())
                .limit(request.getLimit() != null ? request.getLimit() : 5)
                .build();

        List<ArticleResponse> articles = strategyFactory.getStrategy(intent).retrieve(criteria);
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
        return NewsResponse.of(intent.name().toLowerCase(), articles);
    }
}
