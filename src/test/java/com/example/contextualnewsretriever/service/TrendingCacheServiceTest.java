package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.config.CacheConfig;
import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrendingCacheServiceTest {

    private TrendingCacheService cacheService;

    @BeforeEach
    void setUp() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CacheConfig.TRENDING_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder());
        cacheService = new TrendingCacheService(cacheManager);
    }

    @Test
    void putThenGetReturnsSameValue() {
        List<ArticleResponse> value = List.of(ArticleResponse.builder().id("a1").build());
        cacheService.put(28.61, 77.20, 10, value);

        assertThat(cacheService.get(28.61, 77.20, 10)).isEqualTo(value);
    }

    @Test
    void nearbyCoordinatesShareTheSameBucket() {
        List<ArticleResponse> value = List.of(ArticleResponse.builder().id("a1").build());
        cacheService.put(28.6139, 77.2090, 10, value);

        assertThat(cacheService.get(28.62, 77.21, 10)).isEqualTo(value);
    }

    @Test
    void invalidateAllClearsCache() {
        List<ArticleResponse> value = List.of(ArticleResponse.builder().id("a1").build());
        cacheService.put(28.61, 77.20, 10, value);
        cacheService.invalidateAll();

        assertThat(cacheService.get(28.61, 77.20, 10)).isNull();
    }

    @Test
    void missReturnsNull() {
        assertThat(cacheService.get(0.0, 0.0, 10)).isNull();
    }
}
