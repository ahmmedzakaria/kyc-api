package com.nexacore.servicesmodule.cacheservice.service.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisCacheService implements CacheService {

    private final RedisTemplate<String, Object> redisCacheTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cache.redis.key-prefix:nexacore}")
    private String keyPrefix;

    @Override
    public void put(String key, Object value) {
        redisCacheTemplate.opsForValue().set(cacheKey(key), value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            put(key, value);
            return;
        }
        redisCacheTemplate.opsForValue().set(cacheKey(key), value, ttl);
    }

    @Override
    public Optional<Object> get(String key) {
        return Optional.ofNullable(redisCacheTemplate.opsForValue().get(cacheKey(key)));
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        return get(key).map(value -> objectMapper.convertValue(value, type));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisCacheTemplate.hasKey(cacheKey(key)));
    }

    @Override
    public boolean expire(String key, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return false;
        }
        return Boolean.TRUE.equals(redisCacheTemplate.expire(cacheKey(key), ttl));
    }

    @Override
    public Duration getTimeToLive(String key) {
        Long seconds = redisCacheTemplate.getExpire(cacheKey(key));
        if (seconds == null || seconds < 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(seconds);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisCacheTemplate.delete(cacheKey(key)));
    }

    @Override
    public long delete(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisCacheTemplate.delete(keys.stream().map(this::cacheKey).toList());
        return deleted == null ? 0L : deleted;
    }

    @Override
    public long deleteByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(cacheKey(pattern))
                .count(1000)
                .build();

        try (Cursor<String> cursor = redisCacheTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to scan Redis cache keys", e);
        }

        if (keys.isEmpty()) {
            return 0L;
        }
        Long deleted = redisCacheTemplate.delete(keys);
        return deleted == null ? 0L : deleted;
    }

    private String cacheKey(String key) {
        String normalizedKey = Optional.ofNullable(key)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Cache key must not be blank"));
        String normalizedPrefix = Optional.ofNullable(keyPrefix)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .orElse("nexacore");
        return normalizedPrefix + ":" + normalizedKey;
    }
}
