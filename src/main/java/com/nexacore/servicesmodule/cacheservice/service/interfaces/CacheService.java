package com.nexacore.servicesmodule.cacheservice.service.interfaces;

import com.nexacore.servicesmodule.cacheservice.dto.AtomicCounterResult;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;

public interface CacheService {
    AtomicCounterResult increment(String key, Duration initialTtl);

    void put(String key, Object value);

    void put(String key, Object value, Duration ttl);

    Optional<Object> get(String key);

    <T> Optional<T> get(String key, Class<T> type);

    boolean exists(String key);

    boolean expire(String key, Duration ttl);

    Duration getTimeToLive(String key);

    boolean delete(String key);

    long delete(Collection<String> keys);

    long deleteByPattern(String pattern);
}
