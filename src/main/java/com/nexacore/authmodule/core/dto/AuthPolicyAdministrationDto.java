package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;

import java.time.LocalDateTime;

public record AuthPolicyAdministrationDto(
        Long id,
        Long tenantId,
        String clientCode,
        LoginMethod loginMethod,
        LoginIdentifierType loginIdentifierType,
        boolean enabled,
        String version,
        LocalDateTime updatedAt
) {
}
