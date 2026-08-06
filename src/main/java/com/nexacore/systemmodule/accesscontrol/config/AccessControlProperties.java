package com.nexacore.systemmodule.accesscontrol.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@ConfigurationProperties(prefix = "access-control")
@Getter
@Setter
@Slf4j
public class AccessControlProperties {

    private final Environment environment;

    private boolean enabled = true;
    private EnforcementMode enforcementMode = EnforcementMode.REPORT;
    private boolean requireClientForBrowser;
    private boolean requireClientForConfidential = true;
    private boolean registryCoverageEnabled = true;
    private String trustedProxyCidrs;
    private boolean rateLimitEnabled = true;
    private boolean rateLimitFailOpen;

    public AccessControlProperties(Environment environment) {
        this.environment = environment;
    }

    public boolean isDecisionEvaluationEnabled() {
        return enabled && enforcementMode != EnforcementMode.DISABLED;
    }

    @PostConstruct
    void validateProductionMode() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
        if (production && (!enabled || enforcementMode == EnforcementMode.DISABLED)) {
            throw new IllegalStateException("Access control cannot be disabled in a production profile");
        }
        if (!enabled || enforcementMode == EnforcementMode.DISABLED) {
            log.warn("Access-control authorization is disabled; only baseline authentication remains active");
        }
    }
}
