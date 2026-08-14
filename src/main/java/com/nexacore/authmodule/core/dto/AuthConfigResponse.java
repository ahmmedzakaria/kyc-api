package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import lombok.Builder;

import java.util.Set;

@Builder
public record AuthConfigResponse(
        Long tenantId,
        String clientCode,
        LoginMethod loginMethod,
        LoginIdentifierType loginIdentifierType,
        RegistrationCredentialModel registrationCredentialModel,
        SecondFactorPolicyDto secondFactorPolicy,
        String authPolicyVersion,
        RegistrationMode registrationMode,
        @Deprecated(forRemoval = true)
        Set<RegistrationCredentialModel> enabledRegistrationCredentialModels,
        @Deprecated(forRemoval = true)
        Set<LoginMethod> enabledLoginMethods,
        @Deprecated(forRemoval = true)
        Set<LoginIdentifierType> loginIdentifierTypes,
        UserActivationMode userActivationMode,
        String issuerUri,
        String clientId,
        String redirectUri,
        SsoContextDto sso,
        RegistrationContextDto registration,
        SecurityPolicyContextDto securityPolicy,
        Object layout
) {
}
