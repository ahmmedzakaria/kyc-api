package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Set;

public record EffectiveTenantAccessContext(
        long tenantId,
        String tenantCode,
        String hostname,
        long accountId,
        long clientApplicationId,
        Set<UserScopeAssignment> effectiveScopes
) {
    public EffectiveTenantAccessContext {
        effectiveScopes = effectiveScopes == null ? Set.of() : Set.copyOf(effectiveScopes);
        if (effectiveScopes.isEmpty() || effectiveScopes.stream().anyMatch(scope -> scope.tenantId() != tenantId)) {
            throw new IllegalArgumentException("Effective scopes must belong to the effective tenant");
        }
    }
}
