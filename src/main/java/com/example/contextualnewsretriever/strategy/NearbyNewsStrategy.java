package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.exception.InvalidRequestException;
import com.example.contextualnewsretriever.llm.LlmService;
import com.example.contextualnewsretriever.mapper.ArticleMapper;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.example.contextualnewsretriever.service.DistanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class NearbyNewsStrategy implements NewsRetrievalStrategy {

    private static final double DEFAULT_RADIUS_KM = 50.0;

    private final NewsArticleRepository repository;
    private final DistanceService distanceService;
    private final LlmService llmService;

    @Qualifier("llmExecutor")
    private final TaskExecutor llmExecutor;

    @Override
    public NewsIntent supportedIntent() {
        return NewsIntent.NEARBY;
    }

    @Override
    public List<ArticleResponse> retrieve(RetrievalCriteria criteria) {
        if (criteria.getLatitude() == null || criteria.getLongitude() == null) {
            throw new InvalidRequestException("latitude and longitude are required for the NEARBY intent");
        }
        double lat = criteria.getLatitude();
        double lon = criteria.getLongitude();
        double radiusKm = criteria.getRadiusKm() != null ? criteria.getRadiusKm() : DEFAULT_RADIUS_KM;

        List<ArticleResponse> articles = repository.findAllWithCoordinates().stream()
                .map(article -> withDistance(article, lat, lon))
                .filter(scored -> scored.distanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(Scored::distanceKm))
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

    private Scored withDistance(NewsArticle article, double lat, double lon) {
        double distanceKm = distanceService.haversineKm(lat, lon, article.getLatitude(), article.getLongitude());
        ArticleResponse response = ArticleMapper.toResponse(article);
        response.setDistanceKm(distanceKm);
        return new Scored(distanceKm, response);
    }

    private record Scored(double distanceKm, ArticleResponse response) {
    }
}
