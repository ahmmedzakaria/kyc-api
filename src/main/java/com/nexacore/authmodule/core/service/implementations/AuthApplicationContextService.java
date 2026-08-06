package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.RegistrationContextDto;
import com.nexacore.authmodule.core.dto.SecurityPolicyContextDto;
import com.nexacore.authmodule.core.dto.SsoContextDto;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.security.config.KeycloakProperties;
import com.nexacore.authmodule.security.config.RegistrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthApplicationContextService {

    private static final List<String> DEFAULT_SSO_SCOPES = List.of("openid", "profile", "email");

    private final AuthenticationProperties authenticationProperties;
    private final RegistrationProperties registrationProperties;
    private final KeycloakProperties keycloakProperties;
    private final AuthClientPolicyService authClientPolicyService;

    public ApplicationContextDto buildPublicContext(String origin) {
        return buildPublicContext(origin, null);
    }

    public ApplicationContextDto buildPublicContext(String origin, String clientCode) {
        return applyAuthPolicy(ApplicationContextDto.builder()
                .clientCode(resolvePolicyClientCode(origin, clientCode, null))
                .build(), origin, clientCode);
    }

    public ApplicationContextDto applyAuthPolicy(ApplicationContextDto context, String origin) {
        return applyAuthPolicy(context, origin, null);
    }

    public ApplicationContextDto applyAuthPolicy(ApplicationContextDto context, String origin, String requestedClientCode) {
        String redirectUri = buildRedirectUri(origin);
        String policyClientCode = resolvePolicyClientCode(origin, requestedClientCode, context.getClientCode());

        return ApplicationContextDto.builder()
                .clientCode(policyClientCode)
                .clientType(context.getClientType())
                .registrationMode(registrationProperties.getMode())
                .enabledRegistrationCredentialModels(new LinkedHashSet<>(authClientPolicyService.resolveRegistrationCredentialModels(policyClientCode)))
                .enabledLoginMethods(new LinkedHashSet<>(authClientPolicyService.resolveLoginMethods(policyClientCode)))
                .loginIdentifierTypes(new LinkedHashSet<>(authClientPolicyService.resolveLoginIdentifierTypes(policyClientCode)))
                .userActivationMode(registrationProperties.getActivationMode())
                .sso(buildSsoContext(origin, redirectUri, policyClientCode))
                .registration(buildRegistrationContext())
                .securityPolicy(buildSecurityPolicyContext(policyClientCode))
                .privilegeCodes(context.getPrivilegeCodes())
                .enabledModules(context.getEnabledModules())
                .enabledSubmodules(context.getEnabledSubmodules())
                .enabledFeatures(context.getEnabledFeatures())
                .routePolicies(context.getRoutePolicies())
                .uiPolicies(context.getUiPolicies())
                .layout(context.getLayout())
                .build();
    }

    public SsoContextDto buildSsoContext(String origin, String redirectUri) {
        return buildSsoContext(origin, redirectUri, resolvePolicyClientCode(origin, null, null));
    }

    public SsoContextDto buildSsoContext(String origin, String redirectUri, String clientCode) {
        return SsoContextDto.builder()
                .enabled(authClientPolicyService.isLoginMethodEnabled(LoginMethod.SSO, clientCode))
                .issuerUri(keycloakProperties.getIssuerUri())
                .clientId(resolveClientId(origin))
                .redirectUri(redirectUri)
                .logoutRedirectUri(buildLogoutRedirectUri(origin))
                .scopes(new ArrayList<>(DEFAULT_SSO_SCOPES))
                .pkceRequired(true)
                .build();
    }

    public RegistrationContextDto buildRegistrationContext() {
        RegistrationMode mode = registrationProperties.getMode();
        UserActivationMode activationMode = registrationProperties.getActivationMode();
        return RegistrationContextDto.builder()
                .enabled(!RegistrationMode.DISABLED.equals(mode))
                .mode(mode)
                .requiresExistingPerson(registrationProperties.isRequiresExistingPerson())
                .requiresApproval(RegistrationMode.APPROVAL_REQUIRED.equals(mode)
                        || UserActivationMode.APPROVAL_REQUIRED.equals(activationMode))
                .requiresInvite(RegistrationMode.INVITE_ONLY.equals(mode)
                        || UserActivationMode.INVITE_ACCEPTANCE_REQUIRED.equals(activationMode))
                .requiresEmailVerification(UserActivationMode.EMAIL_VERIFICATION_REQUIRED.equals(activationMode))
                .requiresMobileVerification(UserActivationMode.MOBILE_VERIFICATION_REQUIRED.equals(activationMode))
                .allowedPersonTypes(registrationProperties.getAllowedPersonTypes())
                .requiredFields(registrationProperties.getRequiredFields())
                .requiredDocuments(registrationProperties.getRequiredDocuments())
                .build();
    }

    public SecurityPolicyContextDto buildSecurityPolicyContext() {
        return buildSecurityPolicyContext(null);
    }

    public SecurityPolicyContextDto buildSecurityPolicyContext(String clientCode) {
        return SecurityPolicyContextDto.builder()
                .passwordLoginEnabled(authClientPolicyService.isLoginMethodEnabled(LoginMethod.PASSWORD, clientCode))
                .otpLoginEnabled(authClientPolicyService.isLoginMethodEnabled(LoginMethod.OTP, clientCode))
                .ssoLoginEnabled(authClientPolicyService.isLoginMethodEnabled(LoginMethod.SSO, clientCode))
                .sessionTimeoutSeconds(authenticationProperties.getSessionTimeoutSeconds())
                .refreshTokenEnabled(authenticationProperties.isRefreshTokenEnabled())
                .maxLoginAttempts(authenticationProperties.getMaxLoginAttempts())
                .passwordPolicyCode(authenticationProperties.getPasswordPolicyCode())
                .build();
    }

    public String buildRedirectUri(String origin) {
        String baseOrigin = StringUtils.hasText(origin) ? origin : "http://localhost:4200";
        return baseOrigin + "/sso/callback";
    }

    public String buildLogoutRedirectUri(String origin) {
        String baseOrigin = StringUtils.hasText(origin) ? origin : "http://localhost:4200";
        return baseOrigin + "/login";
    }

    public String resolveClientId(String origin) {
        if (StringUtils.hasText(origin) && origin.contains(":4300")) {
            return "privilege-frontend";
        }
        return keycloakProperties.getClientId();
    }

    public String resolvePolicyClientCode(String origin, String requestedClientCode, String contextClientCode) {
        if (StringUtils.hasText(contextClientCode)) {
            return contextClientCode;
        }
        if (StringUtils.hasText(requestedClientCode)) {
            return requestedClientCode.trim();
        }
        return resolveClientId(origin);
    }
}
