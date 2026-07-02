package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.entity.UserArticleEvent;
import com.example.contextualnewsretriever.enums.EventType;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.example.contextualnewsretriever.repository.UserArticleEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrendingServiceTest {

    @Mock
    private UserArticleEventRepository eventRepository;
    @Mock
    private NewsArticleRepository articleRepository;
    @Mock
    private TrendingCacheService cacheService;

    private final DistanceService distanceService = new DistanceService();

    @Test
    void closerAndMoreRecentEventsScoreHigher() {
        TrendingService trendingService = new TrendingService(eventRepository, articleRepository, distanceService, cacheService);

        NewsArticle nearArticle = NewsArticle.builder().id("near").title("Near").latitude(28.61).longitude(77.20).build();
        NewsArticle farArticle = NewsArticle.builder().id("far").title("Far").latitude(19.07).longitude(72.87).build();

        UserArticleEvent recentClickNear = UserArticleEvent.builder()
                .articleId("near").eventType(EventType.CLICK)
                .latitude(28.61).longitude(77.20)
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        UserArticleEvent oldViewFar = UserArticleEvent.builder()
                .articleId("far").eventType(EventType.VIEW)
                .latitude(28.61).longitude(77.20)
                .createdAt(LocalDateTime.now().minusHours(40))
                .build();

        when(cacheService.get(any(Double.class), any(Double.class), any(Integer.class))).thenReturn(null);
        when(eventRepository.findRecentEvents(any())).thenReturn(List.of(recentClickNear, oldViewFar));
        when(articleRepository.findAllById(any())).thenReturn(List.of(nearArticle, farArticle));

        List<ArticleResponse> trending = trendingService.getTrending(28.61, 77.20, 10);

        assertThat(trending).hasSize(2);
        assertThat(trending.get(0).getId()).isEqualTo("near");
        assertThat(trending.get(0).getTrendingScore()).isGreaterThan(trending.get(1).getTrendingScore());
    }

    @Test
    void cachedResultIsReturnedWithoutRecomputing() {
        TrendingService trendingService = new TrendingService(eventRepository, articleRepository, distanceService, cacheService);
        List<ArticleResponse> cached = List.of(ArticleResponse.builder().id("cached").build());
        when(cacheService.get(28.61, 77.20, 10)).thenReturn(cached);

        List<ArticleResponse> result = trendingService.getTrending(28.61, 77.20, 10);

        assertThat(result).isSameAs(cached);
    }
}
