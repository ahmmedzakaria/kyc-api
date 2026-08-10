package com.nexacore.authmodule.core.metrics;

import com.nexacore.authmodule.core.repository.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthFoundationMetrics {
    public AuthFoundationMetrics(MeterRegistry registry,
                                 UserRepository userRepository,
                                 com.nexacore.authmodule.core.repository.AuthUserBackfillQuarantineRepository quarantineRepository) {
        Gauge.builder("nexacore.auth.legacy.users.missing_tenant", userRepository,
                        repository -> repository.countByTenantIdIsNull())
                .description("Legacy Auth users without a tenant account owner")
                .register(registry);
        Gauge.builder("nexacore.auth.legacy.users.missing_normalized_username", userRepository,
                        repository -> repository.countByNormalizedUsernameIsNull())
                .description("Auth users without a normalized username")
                .register(registry);
        Gauge.builder("nexacore.auth.backfill.users.quarantined", quarantineRepository,
                        repository -> repository.countByResolvedFalse())
                .description("Unresolved Auth users quarantined during tenant backfill")
                .register(registry);
    }
}
