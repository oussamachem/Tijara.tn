package com.smartboutique.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;

/**
 * Cache Redis du fil marketplace / catégories (données PUBLIQUES, non liées à un user) pour tenir la
 * montée en charge. Valeurs sérialisées en JSON (compatible records). Résilient : si Redis est
 * indisponible, on IGNORE le cache et on exécute la méthode -> le site continue de marcher.
 * Le plafond mémoire (maxmemory 500mb + eviction) est fixé côté serveur Redis (docker-compose).
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements org.springframework.cache.annotation.CachingConfigurer {

    /** TTL + sérialisation JSON de TOUS les caches (repris par le RedisCacheManager auto). Le
     *  sérialiseur par défaut gère le typage (@class) de façon cohérente écriture/lecture -> les
     *  records (PageResponse, FeedProductResponse) et listes se relisent correctement. */
    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(60))                 // fraîcheur : 1 min
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    /** Redis KO -> on log et on continue SANS cache (jamais d'erreur 500 à cause du cache). */
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override public void handleCacheGetError(RuntimeException e, Cache cache, Object key) { warn("get", e); }
            @Override public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) { warn("put", e); }
            @Override public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) { warn("evict", e); }
            @Override public void handleCacheClearError(RuntimeException e, Cache cache) { warn("clear", e); }
            private void warn(String op, RuntimeException e) {
                log.warn("[cache] Redis indisponible ({}), exécution sans cache : {}", op, e.getMessage());
            }
        };
    }
}
