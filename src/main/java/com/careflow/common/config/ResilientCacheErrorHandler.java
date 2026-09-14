package com.careflow.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Resilient cache error handler providing graceful degradation when Redis is unavailable (§50, §76 failure scenario 10).
 * <p>
 * If the cache engine (Redis) experiences connection timeout, connection refusal, or serialization errors:
 * <ul>
 *   <li>Cache read failures log a WARN and treat the request as a cache miss, seamlessly falling back to the database.</li>
 *   <li>Cache write, evict, and clear failures log a WARN and allow the business transaction to complete successfully.</li>
 * </ul>
 */
public class ResilientCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ResilientCacheErrorHandler.class);

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache read error on cache '{}' for key '{}'. Gracefully falling back to database: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis cache put error on cache '{}' for key '{}'. Business transaction proceeds: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache evict error on cache '{}' for key '{}'. Business transaction proceeds: {}",
                cache != null ? cache.getName() : "unknown", key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache clear error on cache '{}'. Business transaction proceeds: {}",
                cache != null ? cache.getName() : "unknown", exception.getMessage());
    }
}
