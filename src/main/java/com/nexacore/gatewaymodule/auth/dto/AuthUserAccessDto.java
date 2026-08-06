package com.nexacore.gatewaymodule.auth.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record AuthUserAccessDto(
        Long userId,
        Long personId,
        Long tenantId,
        Long businessId,
        Long branchId,
        Set<Long> roleIds,
        boolean admin
) {
}
