package com.nexacore.authmodule.core.dto;

import java.util.Set;

public record EffectiveTenantContextDto(
        Long tenantId,
        String tenantCode,
        String hostname,
        Set<EffectiveScopeAssignmentDto> scopeAssignments
) {
    public EffectiveTenantContextDto {
        scopeAssignments = scopeAssignments == null ? Set.of() : Set.copyOf(scopeAssignments);
    }
}
