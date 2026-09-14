package com.careflow.common.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResilientCacheErrorHandlerTest {

    private ResilientCacheErrorHandler errorHandler;
    private Cache cache;

    @BeforeEach
    void setUp() {
        errorHandler = new ResilientCacheErrorHandler();
        cache = mock(Cache.class);
        when(cache.getName()).thenReturn("test-cache");
    }

    @Test
    @DisplayName("handleCacheGetError suppresses exception and allows graceful database degradation")
    void handleCacheGetError_DoesNotThrowException() {
        RuntimeException redisException = new RuntimeException("Redis connection refused to localhost:6379");

        assertThatCode(() -> errorHandler.handleCacheGetError(redisException, cache, "key-123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCachePutError suppresses exception and does not abort business transaction")
    void handleCachePutError_DoesNotThrowException() {
        RuntimeException redisException = new RuntimeException("Redis write timeout");

        assertThatCode(() -> errorHandler.handleCachePutError(redisException, cache, "key-123", "value"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheEvictError suppresses exception and does not abort transaction")
    void handleCacheEvictError_DoesNotThrowException() {
        RuntimeException redisException = new RuntimeException("Redis eviction failure");

        assertThatCode(() -> errorHandler.handleCacheEvictError(redisException, cache, "key-123"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("handleCacheClearError suppresses exception")
    void handleCacheClearError_DoesNotThrowException() {
        RuntimeException redisException = new RuntimeException("Redis cluster unavailable");

        assertThatCode(() -> errorHandler.handleCacheClearError(redisException, cache))
                .doesNotThrowAnyException();
    }
}
