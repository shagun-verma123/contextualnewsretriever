package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.config.CacheConfig;
import com.example.contextualnewsretriever.dto.ArticleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper around the Caffeine-backed "trending" cache, keyed by rounded lat/lon buckets
 * plus limit so nearby requests share a cache entry. Bypasses @Cacheable in favor of manual
 * get/put/invalidate calls so a new event can proactively evict the whole cache.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingCacheService {

    private static final double BUCKET_SIZE_DEGREES = 0.5;

    private final CacheManager cacheManager;

    public List<ArticleResponse> get(double lat, double lon, int limit) {
        String cacheKey = buildKey(lat, lon, limit);
        log.info("Trying to fetch info for key : "+cacheKey+" lat : "+lat+" lon : "+lon);
        Cache.ValueWrapper wrapper = cache().get(cacheKey);
        return wrapper != null ? (List<ArticleResponse>) wrapper.get() : null;
    }

    public void put(double lat, double lon, int limit, List<ArticleResponse> value) {
        cache().put(buildKey(lat, lon, limit), value);
    }

    public void invalidateAll() {
        cache().clear();
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(CacheConfig.TRENDING_CACHE);
        if (cache == null) {
            throw new IllegalStateException("Trending cache is not configured");
        }
        return cache;
    }

    private String buildKey(double lat, double lon, int limit) {
        long latBucket = Math.round(lat / BUCKET_SIZE_DEGREES);
        long lonBucket = Math.round(lon / BUCKET_SIZE_DEGREES);
        return "trending:" + latBucket + ":" + lonBucket + ":limit:" + limit;
    }
}
