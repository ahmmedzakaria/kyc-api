package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;

public record ResolvedAuthPolicy(
        long tenantId,
        String clientCode,
        LoginMethod loginMethod,
        LoginIdentifierType loginIdentifierType,
        RegistrationCredentialModel registrationCredentialModel,
        String policyVersion
) {
}
