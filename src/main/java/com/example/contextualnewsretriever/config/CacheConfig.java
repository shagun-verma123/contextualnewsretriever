package com.example.contextualnewsretriever.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TRENDING_CACHE = "trending";

    @Bean
    public CacheManager cacheManager(@Value("${trending.cache.ttl-minutes:5}") long ttlMinutes) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(TRENDING_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .maximumSize(1000));
        return cacheManager;
    }
}
