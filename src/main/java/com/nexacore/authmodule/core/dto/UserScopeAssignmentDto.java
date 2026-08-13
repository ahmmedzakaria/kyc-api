package com.nexacore.authmodule.core.dto;

public record UserScopeAssignmentDto(Long tenantId, Long businessId, Long branchId, boolean active) {
    public UserScopeAssignmentDto {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (branchId != null && businessId == null) {
            throw new IllegalArgumentException("businessId is required when branchId is assigned");
        }
    }
}
