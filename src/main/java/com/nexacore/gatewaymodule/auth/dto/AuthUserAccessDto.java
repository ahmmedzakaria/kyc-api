package com.nexacore.gatewaymodule.auth.dto;

import lombok.Builder;

import java.util.Set;

@Builder
public record AuthUserAccessDto(
        Long userId,
        Long personId,
        Set<Long> roleIds,
        boolean admin
) {
}
