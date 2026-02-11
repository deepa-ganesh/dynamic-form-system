package com.dynamicform.form.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis cache configuration.
 *
 * Configures caching for:
 * - Form schemas (long TTL - 1 hour)
 * - Active schema (medium TTL - 30 minutes)
 * - Lookup data (long TTL - 1 hour)
 *
 * Benefits:
 * - Reduced database queries
 * - Faster response times
 * - Lower load on PostgreSQL
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    /**
     * Configure Redis cache manager with custom TTL per cache.
     *
     * @param connectionFactory Redis connection factory (auto-configured)
     * @return configured CacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis cache manager");

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer(objectMapper())
                )
            )
            .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("schemas", defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("activeSchema", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put("lookupData", defaultConfig.entryTtl(Duration.ofHours(1)));

        log.info("Configured caches: schemas (1h), activeSchema (30m), lookupData (1h)");

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }

    /**
     * Clear cache regions at startup.
     *
     * This avoids class-cast issues if cache entries were previously serialized
     * without type metadata.
     *
     * @param cacheManager cache manager
     * @return startup runner
     */
    @Bean
    public ApplicationRunner cacheStartupCleaner(CacheManager cacheManager) {
        return args -> {
            for (String cacheName : List.of("schemas", "activeSchema", "lookupData")) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            }
            log.info("Cleared cache regions on startup");
        };
    }

    /**
     * ObjectMapper for Redis JSON serialization.
     * Configured to handle Java 8 date/time types.
     *
     * @return configured ObjectMapper
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.dynamicform")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
