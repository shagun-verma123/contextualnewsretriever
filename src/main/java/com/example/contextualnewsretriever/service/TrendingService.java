package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.entity.UserArticleEvent;
import com.example.contextualnewsretriever.enums.EventType;
import com.example.contextualnewsretriever.mapper.ArticleMapper;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.example.contextualnewsretriever.repository.UserArticleEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trending score per article = sum over its recent events of:
 *   weight(eventType) * recencyMultiplier(now - event.createdAt) * proximityMultiplier(distance)
 * Only events from the last 48h are considered; weights/decay curves are intentionally simple
 * (linear/step) so the formula stays easy to explain and verify by hand.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingService {

    private static final int LOOKBACK_HOURS = 48;
    private static final double VIEW_WEIGHT = 1.0;
    private static final double CLICK_WEIGHT = 3.0;
    private static final double PROXIMITY_RADIUS_KM = 100.0;

    private final UserArticleEventRepository eventRepository;
    private final NewsArticleRepository articleRepository;
    private final DistanceService distanceService;
    private final TrendingCacheService cacheService;

    public List<ArticleResponse> getTrending(double lat, double lon, int limit) {
        List<ArticleResponse> cached = cacheService.get(lat, lon, limit);
        if (cached != null) {
            log.info("Cache Hit Successful for  lat : "+lat+" lon : "+lon);
            return cached;
        }
        log.info("Cache Miss for  lat : "+lat+" lon : "+lon);
        List<ArticleResponse> computed = computeTrending(lat, lon, limit);
        cacheService.put(lat, lon, limit, computed);
        return computed;
    }

    public void recordEvent(UserArticleEvent event) {
        eventRepository.save(event);
        cacheService.invalidateAll();
    }

    private List<ArticleResponse> computeTrending(double lat, double lon, int limit) {
        LocalDateTime since = LocalDateTime.now().minusHours(LOOKBACK_HOURS);
        List<UserArticleEvent> recentEvents = eventRepository.findRecentEvents(since);

        Map<String, Double> scoresByArticleId = new HashMap<>();
        for (UserArticleEvent event : recentEvents) {
            double score = eventScore(event, lat, lon);
            scoresByArticleId.merge(event.getArticleId(), score, Double::sum);
        }

        Map<String, NewsArticle> articlesById = new HashMap<>();
        articleRepository.findAllById(scoresByArticleId.keySet())
                .forEach(article -> articlesById.put(article.getId(), article));

        return scoresByArticleId.entrySet().stream()
                .filter(entry -> articlesById.containsKey(entry.getKey()))
                .map(entry -> {
                    ArticleResponse response = ArticleMapper.toResponse(articlesById.get(entry.getKey()));
                    response.setTrendingScore(entry.getValue());
                    return response;
                })
                .sorted(Comparator.comparingDouble(ArticleResponse::getTrendingScore).reversed())
                .limit(limit)
                .toList();
    }

    private double eventScore(UserArticleEvent event, double lat, double lon) {
        double weight = event.getEventType() == EventType.CLICK ? CLICK_WEIGHT : VIEW_WEIGHT;
        double recency = recencyMultiplier(event.getCreatedAt());
        double proximity = proximityMultiplier(event, lat, lon);
        return weight * recency * proximity;
    }

    private double recencyMultiplier(LocalDateTime createdAt) {
        double hoursAgo = Duration.between(createdAt, LocalDateTime.now()).toMinutes() / 60.0;
        if (hoursAgo <= 1) {
            return 1.0;
        }
        if (hoursAgo >= LOOKBACK_HOURS) {
            return 0.0;
        }
        return 1.0 - (hoursAgo / LOOKBACK_HOURS);
    }

    private double proximityMultiplier(UserArticleEvent event, double lat, double lon) {
        if (event.getLatitude() == null || event.getLongitude() == null) {
            return 0.5;
        }
        double distanceKm = distanceService.haversineKm(lat, lon, event.getLatitude(), event.getLongitude());
        if (distanceKm >= PROXIMITY_RADIUS_KM) {
            return 0.1;
        }
        return 1.0 - (distanceKm / PROXIMITY_RADIUS_KM) * 0.9;
    }
}
