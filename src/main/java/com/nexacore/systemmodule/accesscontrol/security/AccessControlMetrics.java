package com.nexacore.systemmodule.accesscontrol.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Low-cardinality operational metrics for the access-control decision path. */
@Component
@RequiredArgsConstructor
public class AccessControlMetrics {
    static final String DECISIONS = "nexacore.access.control.requests";
    static final String DENIALS = "nexacore.access.control.denials";
    static final String REGISTRY_RESOLUTION = "nexacore.access.control.registry.resolution";
    static final String CACHE_REQUESTS = "nexacore.access.control.cache.requests";

    private final MeterRegistry meterRegistry;

    @EventListener
    public void onAuthorizationDecision(AuthorizationDecisionEvent event) {
        String decision = normalized(event.decision(), "UNKNOWN");
        Counter.builder(DECISIONS).description("Access-control evaluated requests")
                .tag("decision", decision).register(meterRegistry).increment();
        if ("DENIED".equals(decision)) {
            String reason = normalized(event.denialCode(), "UNKNOWN");
            Counter.builder(DENIALS).description("Access-control denied requests by reason")
                    .tag("reason", reason)
                    .tag("api_code", normalized(event.apiCode(), "UNRESOLVED"))
                    .register(meterRegistry).increment();
        }
    }

    public void recordRegistryResolution(long durationNanos, String outcome) {
        Timer.builder(REGISTRY_RESOLUTION).description("API registry route-resolution latency")
                .tag("outcome", normalized(outcome, "ERROR")).register(meterRegistry)
                .record(Math.max(0, durationNanos), TimeUnit.NANOSECONDS);
    }

    public void recordCacheRequest(String cache, String result) {
        Counter.builder(CACHE_REQUESTS).description("Authorization cache requests by result")
                .tag("cache", cache).tag("result", result).register(meterRegistry).increment();
    }

    private String normalized(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }
}
