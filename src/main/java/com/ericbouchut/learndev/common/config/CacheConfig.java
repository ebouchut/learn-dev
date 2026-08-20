package com.ericbouchut.learndev.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-memory caching (Spring Cache over Caffeine), currently holding rendered
 * lesson HTML (see ADR-0013 in docs/adr/).
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String RENDERED_MARKDOWN_CACHE = "renderedMarkdown";

    // Entries are content-addressed (SHA-256 keys), so they never go stale;
    // the only pressure is memory, capped by entry count with LRU eviction.
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(RENDERED_MARKDOWN_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(1_000));
        return cacheManager;
    }
}
