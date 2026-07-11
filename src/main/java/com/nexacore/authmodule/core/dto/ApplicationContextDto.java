package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationContextDto {
    private String clientCode;
    private String clientType;
    private RegistrationMode registrationMode;
    private UserActivationMode userActivationMode;
    private SsoContextDto sso;
    private RegistrationContextDto registration;
    private SecurityPolicyContextDto securityPolicy;

    @Builder.Default
    private Set<RegistrationCredentialModel> enabledRegistrationCredentialModels = new HashSet<>();

    @Builder.Default
    private Set<LoginMethod> enabledLoginMethods = new HashSet<>();

    @Builder.Default
    private Set<LoginIdentifierType> loginIdentifierTypes = new HashSet<>();

    @Builder.Default
    private List<SidebarMenuDto> menus = new ArrayList<>();

    @Builder.Default
    private Set<String> privilegeCodes = new HashSet<>();

    @Builder.Default
    private Set<String> enabledModules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledSubmodules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledFeatures = new HashSet<>();
}
