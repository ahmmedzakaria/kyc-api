package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;

public record AuthPolicyAdministrationRequest(
        Long tenantId,
        String clientCode,
        LoginMethod loginMethod,
        LoginIdentifierType loginIdentifierType,
        Boolean enabled
) {
}
