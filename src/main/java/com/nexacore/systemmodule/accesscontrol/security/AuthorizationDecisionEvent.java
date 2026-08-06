package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Set;

public record AuthorizationDecisionEvent(
        String eventType,
        String traceId,
        String httpMethod,
        String apiCode,
        String pathTemplate,
        String clientCode,
        Long userId,
        String decision,
        String denialCode,
        String requiredPrivilegeCode,
        Set<Long> tenantIds,
        Set<Long> businessIds,
        Set<Long> branchIds,
        long durationMicros
) {
    public AuthorizationDecisionEvent {
        eventType = "AUTHORIZATION_DECISION";
        tenantIds = tenantIds == null ? Set.of() : Set.copyOf(tenantIds);
        businessIds = businessIds == null ? Set.of() : Set.copyOf(businessIds);
        branchIds = branchIds == null ? Set.of() : Set.copyOf(branchIds);
        durationMicros = Math.max(0, durationMicros);
    }
}
