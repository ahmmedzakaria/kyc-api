package com.nexacore.authmodule.core.metrics;

import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthCutoverComparisonTelemetry {
    private final UserRepository users;
    private final boolean enabled;
    private final Counter match;
    private final Counter mismatch;

    public AuthCutoverComparisonTelemetry(UserRepository users, MeterRegistry registry,
            @Value("${nexacore.auth.cutover.dual-read-comparison-enabled:true}") boolean enabled) {
        this.users = users;
        this.enabled = enabled;
        this.match = registry.counter("nexacore.auth.cutover.dual_read", "result", "match");
        this.mismatch = registry.counter("nexacore.auth.cutover.dual_read", "result", "mismatch");
    }

    public void compare(AuthUser tenantAccount) {
        if (!enabled) return;
        boolean same = users.findByUsername(tenantAccount.getUsername())
                .map(legacy -> legacy.getId().equals(tenantAccount.getId()))
                .orElse(false);
        (same ? match : mismatch).increment();
    }
}
