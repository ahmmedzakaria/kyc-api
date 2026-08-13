package com.nexacore.systemmodule.accesscontrol.dto;

public record ClientScopeAssignmentDto(Long tenantId, Long businessId, Long branchId, boolean active) {
    public ClientScopeAssignmentDto(Long tenantId,Long businessId,Long branchId) { this(tenantId,businessId,branchId,true); }
    public ClientScopeAssignmentDto {
        if (tenantId == null) throw new IllegalArgumentException("tenantId is required");
        if (!active) throw new IllegalArgumentException("Scope replacements must be active");
        if (branchId != null && businessId == null) {
            throw new IllegalArgumentException("businessId is required when branchId is assigned");
        }
    }
}
