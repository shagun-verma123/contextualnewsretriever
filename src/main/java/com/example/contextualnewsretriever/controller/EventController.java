package com.example.contextualnewsretriever.controller;

import com.example.contextualnewsretriever.dto.UserEventRequest;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.entity.UserArticleEvent;
import com.example.contextualnewsretriever.enums.EventType;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.example.contextualnewsretriever.service.TrendingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class EventController {

    private final TrendingService trendingService;
    private final NewsArticleRepository articleRepository;

    @PostMapping("/events")
    public ResponseEntity<Void> recordEvent(@Valid @RequestBody UserEventRequest request) {
        UserArticleEvent event = UserArticleEvent.builder()
                .userId(request.getUserId())
                .articleId(request.getArticleId())
                .eventType(request.getEventType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        trendingService.recordEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/events/simulate")
    public Map<String, Integer> simulate(@RequestParam(defaultValue = "100") @Min(1) @Max(5000) int count) {
        List<NewsArticle> articles = articleRepository.findAllWithCoordinates();
        if (articles.isEmpty()) {
            return Map.of("simulated", 0);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            NewsArticle article = articles.get(random.nextInt(articles.size()));
            EventType eventType = random.nextBoolean() ? EventType.VIEW : EventType.CLICK;
            double jitterLat = article.getLatitude() + random.nextDouble(-0.2, 0.2);
            double jitterLon = article.getLongitude() + random.nextDouble(-0.2, 0.2);

            UserArticleEvent event = UserArticleEvent.builder()
                    .userId("sim-user-" + random.nextInt(1, 200))
                    .articleId(article.getId())
                    .eventType(eventType)
                    .latitude(jitterLat)
                    .longitude(jitterLon)
                    .build();
            trendingService.recordEvent(event);
        }
        return Map.of("simulated", count);
    }
}
