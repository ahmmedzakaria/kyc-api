package com.nexacore.systemmodule.accesscontrol.security;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AccessControlMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AccessControlMetrics metrics = new AccessControlMetrics(registry);

    @Test
    void recordsAllowedAndDeniedDecisionsWithBoundedOperationalTags() {
        metrics.onAuthorizationDecision(event("ALLOWED", null, "GET_PEOPLE"));
        metrics.onAuthorizationDecision(event("DENIED", "INVALID_CLIENT_CREDENTIALS", "ADMIN_USERS"));

        assertThat(registry.get(AccessControlMetrics.DECISIONS).tag("decision", "ALLOWED").counter().count())
                .isEqualTo(1);
        assertThat(registry.get(AccessControlMetrics.DENIALS)
                .tags("reason", "INVALID_CLIENT_CREDENTIALS", "api_code", "ADMIN_USERS")
                .counter().count()).isEqualTo(1);
    }

    @Test
    void recordsRegistryLatencyAndCacheHitRateInputs() {
        metrics.recordRegistryResolution(2_000, "matched");
        metrics.recordCacheRequest("registry", "hit");
        metrics.recordCacheRequest("registry", "miss");

        assertThat(registry.get(AccessControlMetrics.REGISTRY_RESOLUTION).tag("outcome", "MATCHED")
                .timer().count()).isEqualTo(1);
        assertThat(registry.get(AccessControlMetrics.CACHE_REQUESTS).tags("cache", "registry", "result", "hit")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(AccessControlMetrics.CACHE_REQUESTS).tags("cache", "registry", "result", "miss")
                .counter().count()).isEqualTo(1);
    }

    private AuthorizationDecisionEvent event(String decision, String denialCode, String apiCode) {
        return new AuthorizationDecisionEvent(null, "trace", "GET", apiCode, "/ignored", null, null,
                decision, denialCode, null, Set.of(), Set.of(), Set.of(), 1);
    }
}
