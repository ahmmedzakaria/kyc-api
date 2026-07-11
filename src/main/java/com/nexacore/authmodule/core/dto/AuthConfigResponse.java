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
        RegistrationMode registrationMode,
        Set<RegistrationCredentialModel> enabledRegistrationCredentialModels,
        Set<LoginMethod> enabledLoginMethods,
        Set<LoginIdentifierType> loginIdentifierTypes,
        UserActivationMode userActivationMode,
        String issuerUri,
        String clientId,
        String redirectUri,
        SsoContextDto sso,
        RegistrationContextDto registration,
        SecurityPolicyContextDto securityPolicy,
        ApplicationContextDto applicationContext
) {
}
