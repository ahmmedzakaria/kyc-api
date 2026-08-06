package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Set;

public record AuthenticatedRequestContext(
        Long userId,
        String username,
        Long clientApplicationId,
        String clientCode,
        Long tenantId,
        Long businessId,
        Long branchId,
        String traceId,
        Set<String> effectivePrivilegeCodes
) {
    public AuthenticatedRequestContext {
        effectivePrivilegeCodes = effectivePrivilegeCodes == null
                ? Set.of() : Set.copyOf(effectivePrivilegeCodes);
    }
}
