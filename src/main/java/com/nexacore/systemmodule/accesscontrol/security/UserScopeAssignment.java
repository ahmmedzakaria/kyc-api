package com.nexacore.systemmodule.accesscontrol.security;

public record UserScopeAssignment(
        Long tenantId,
        Long businessId,
        Long branchId
) {
    public UserScopeAssignment {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for a user scope assignment");
        }
        if (branchId != null && businessId == null) {
            throw new IllegalArgumentException("businessId is required when branchId is assigned");
        }
    }
}
