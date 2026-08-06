package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Set;

public record AuthenticatedRequestContext(
        Long userId,
        String username,
        Long clientApplicationId,
        String clientCode,
        Set<UserScopeAssignment> scopeAssignments,
        String traceId,
        Set<String> effectivePrivilegeCodes
) {
    public AuthenticatedRequestContext {
        scopeAssignments = scopeAssignments == null ? Set.of() : Set.copyOf(scopeAssignments);
        effectivePrivilegeCodes = effectivePrivilegeCodes == null
                ? Set.of() : Set.copyOf(effectivePrivilegeCodes);
    }
}
