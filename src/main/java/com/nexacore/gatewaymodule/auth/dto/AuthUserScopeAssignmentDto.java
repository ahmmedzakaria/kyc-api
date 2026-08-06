package com.nexacore.gatewaymodule.auth.dto;

public record AuthUserScopeAssignmentDto(
        Long tenantId,
        Long businessId,
        Long branchId
) {
}
