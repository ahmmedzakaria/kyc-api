package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.servicesmodule.cacheservice.dto.AtomicCounterResult;
import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RedisClientRateLimiterTest {

    private final AccessControlProperties properties = new AccessControlProperties(new MockEnvironment());
    private final StubCacheService cache = new StubCacheService();
    private final RedisClientRateLimiter limiter = new RedisClientRateLimiter(cache, properties);

    @Test
    void allowsWithinLimitAndDeniesAfterLimitUsingAtomicRedisResult() {
        SysAccClientApplication client = client(2);
        cache.result = new AtomicCounterResult(1L, Duration.ofSeconds(59));
        ClientRateLimitDecision first = limiter.check(client, request("POST", "/api/v1/person/search"));
        cache.result = new AtomicCounterResult(3L, Duration.ofSeconds(41));
        ClientRateLimitDecision exceeded = limiter.check(client, request("POST", "/api/v1/person/search"));

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isEqualTo(1);
        assertThat(exceeded.allowed()).isFalse();
        assertThat(exceeded.remaining()).isZero();
        assertThat(exceeded.retryAfterSeconds()).isEqualTo(41);
        assertThat(cache.lastKey).startsWith("rate-limit:client:9:route:");
    }

    @Test
    void skipsRedisWhenClientHasNoLimitOrFeatureIsDisabled() {
        assertThat(limiter.check(client(null), request("GET", "/api/test")).limit()).isZero();
        properties.setRateLimitEnabled(false);
        assertThat(limiter.check(client(2), request("GET", "/api/test")).limit()).isZero();
        assertThat(cache.calls).isZero();
    }

    @Test
    void backendFailureFailsClosedUnlessExplicitlyConfiguredOtherwise() {
        cache.failure = new IllegalStateException("redis down");
        assertThatThrownBy(() -> limiter.check(client(2), request("GET", "/api/test")))
                .isInstanceOf(RateLimitBackendUnavailableException.class);

        properties.setRateLimitFailOpen(true);
        assertThat(limiter.check(client(2), request("GET", "/api/test")).allowed()).isTrue();
    }

    private SysAccClientApplication client(Integer limit) {
        return SysAccClientApplication.builder().id(9L).clientCode("portal")
                .rateLimitPerMinute(limit).build();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static final class StubCacheService implements CacheService {
        private AtomicCounterResult result;
        private RuntimeException failure;
        private String lastKey;
        private int calls;

        @Override
        public AtomicCounterResult increment(String key, Duration initialTtl) {
            calls++;
            lastKey = key;
            if (failure != null) throw failure;
            return result;
        }

        @Override public void put(String key, Object value) { throw new UnsupportedOperationException(); }
        @Override public void put(String key, Object value, Duration ttl) { throw new UnsupportedOperationException(); }
        @Override public Optional<Object> get(String key) { return Optional.empty(); }
        @Override public <T> Optional<T> get(String key, Class<T> type) { return Optional.empty(); }
        @Override public boolean exists(String key) { return false; }
        @Override public boolean expire(String key, Duration ttl) { return false; }
        @Override public Duration getTimeToLive(String key) { return Duration.ZERO; }
        @Override public boolean delete(String key) { return false; }
        @Override public long delete(Collection<String> keys) { return 0; }
        @Override public long deleteByPattern(String pattern) { return 0; }
    }
}
