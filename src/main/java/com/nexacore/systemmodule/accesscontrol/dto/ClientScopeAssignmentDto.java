package com.nexacore.systemmodule.accesscontrol.dto;

public record ClientScopeAssignmentDto(Long tenantId, Long businessId, Long branchId) {
    public ClientScopeAssignmentDto {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (branchId != null && businessId == null) {
            throw new IllegalArgumentException("businessId is required when branchId is assigned");
        }
    }
}
