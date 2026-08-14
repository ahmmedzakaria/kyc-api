package com.nexacore.authmodule.core.dto;

import com.nexacore.commonmodule.dto.RoutePrivilegePolicyDto;
import com.nexacore.commonmodule.dto.UiPrivilegePolicyDto;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationContextDto {
    private Long tenantId;
    private String clientCode;
    private String clientType;
    private EffectiveTenantContextDto effectiveTenant;
    private String authorizationVersion;
    private RegistrationMode registrationMode;
    private UserActivationMode userActivationMode;
    private SsoContextDto sso;
    private RegistrationContextDto registration;
    private SecurityPolicyContextDto securityPolicy;
    private LoginMethod loginMethod;
    private LoginIdentifierType loginIdentifierType;
    private RegistrationCredentialModel registrationCredentialModel;
    private SecondFactorPolicyDto secondFactorPolicy;
    private String authPolicyVersion;

    @Builder.Default
    @Deprecated(forRemoval = true)
    private Set<RegistrationCredentialModel> enabledRegistrationCredentialModels = new HashSet<>();

    @Builder.Default
    @Deprecated(forRemoval = true)
    private Set<LoginMethod> enabledLoginMethods = new HashSet<>();

    @Builder.Default
    @Deprecated(forRemoval = true)
    private Set<LoginIdentifierType> loginIdentifierTypes = new HashSet<>();

    @Builder.Default
    private Set<String> privilegeCodes = new HashSet<>();

    @Builder.Default
    private Set<String> enabledModules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledSubmodules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledFeatures = new HashSet<>();

    @Builder.Default
    private List<RoutePrivilegePolicyDto> routePolicies = new ArrayList<>();

    @Builder.Default
    private List<UiPrivilegePolicyDto> uiPolicies = new ArrayList<>();

    private Object layout;
}
