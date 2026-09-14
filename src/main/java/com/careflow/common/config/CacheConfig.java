package com.careflow.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Enterprise caching configuration with resilient error handling and JSON serialization (§50, §72).
 */
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    public static final String CACHE_SYSTEM_SETTINGS = "system-settings";
    public static final String CACHE_DEPARTMENTS = "departments";
    public static final String CACHE_MEDICATIONS = "medications";
    public static final String CACHE_LAB_TESTS = "lab-tests";

    @Override
    public CacheErrorHandler errorHandler() {
        return new ResilientCacheErrorHandler();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
    public RedisCacheConfiguration defaultRedisCacheConfiguration() {
        ObjectMapper cacheObjectMapper = new ObjectMapper();
        cacheObjectMapper.registerModule(new JavaTimeModule());
        cacheObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaultRedisCacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration(CACHE_SYSTEM_SETTINGS, defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(CACHE_DEPARTMENTS, defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_MEDICATIONS, defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration(CACHE_LAB_TESTS, defaultRedisCacheConfiguration.entryTtl(Duration.ofMinutes(30)));
    }
}
