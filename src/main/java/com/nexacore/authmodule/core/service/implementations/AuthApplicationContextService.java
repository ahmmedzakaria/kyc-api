package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.RegistrationContextDto;
import com.nexacore.authmodule.core.dto.ResolvedAuthPolicy;
import com.nexacore.authmodule.core.dto.SecondFactorPolicyDto;
import com.nexacore.authmodule.core.dto.SecurityPolicyContextDto;
import com.nexacore.authmodule.core.dto.SsoContextDto;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.security.config.KeycloakProperties;
import com.nexacore.authmodule.security.config.RegistrationProperties;
import com.nexacore.authmodule.security.service.TenantAccountResolver;
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
    private final TenantAccountResolver tenantAccountResolver;

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
        long tenantId = context.getEffectiveTenant() != null && context.getEffectiveTenant().tenantId() != null
                ? context.getEffectiveTenant().tenantId()
                : tenantAccountResolver.resolveRequiredTenant(policyClientCode);
        ResolvedAuthPolicy policy = authClientPolicyService.resolveRequiredPolicy(tenantId, policyClientCode);

        return ApplicationContextDto.builder()
                .tenantId(tenantId)
                .clientCode(policy.clientCode())
                .clientType(context.getClientType())
                .effectiveTenant(context.getEffectiveTenant())
                .authorizationVersion(context.getAuthorizationVersion())
                .registrationMode(registrationProperties.getMode())
                .userActivationMode(registrationProperties.getActivationMode())
                .loginMethod(policy.loginMethod())
                .loginIdentifierType(policy.loginIdentifierType())
                .registrationCredentialModel(policy.registrationCredentialModel())
                .secondFactorPolicy(SecondFactorPolicyDto.disabled())
                .authPolicyVersion(policy.policyVersion())
                .enabledRegistrationCredentialModels(new LinkedHashSet<>(List.of(policy.registrationCredentialModel())))
                .enabledLoginMethods(new LinkedHashSet<>(List.of(policy.loginMethod())))
                .loginIdentifierTypes(new LinkedHashSet<>(List.of(policy.loginIdentifierType())))
                .sso(buildSsoContext(origin, redirectUri, policy))
                .registration(buildRegistrationContext())
                .securityPolicy(buildSecurityPolicyContext(policy))
                .privilegeCodes(context.getPrivilegeCodes())
                .enabledModules(context.getEnabledModules())
                .enabledSubmodules(context.getEnabledSubmodules())
                .enabledFeatures(context.getEnabledFeatures())
                .routePolicies(context.getRoutePolicies())
                .uiPolicies(context.getUiPolicies())
                .layout(context.getLayout())
                .build();
    }

    public SsoContextDto buildSsoContext(String origin, String redirectUri, ResolvedAuthPolicy policy) {
        return SsoContextDto.builder()
                .enabled(policy.loginMethod() == LoginMethod.SSO)
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

    public SecurityPolicyContextDto buildSecurityPolicyContext(ResolvedAuthPolicy policy) {
        return SecurityPolicyContextDto.builder()
                .passwordLoginEnabled(policy.loginMethod() == LoginMethod.PASSWORD)
                .otpLoginEnabled(policy.loginMethod() == LoginMethod.OTP)
                .ssoLoginEnabled(policy.loginMethod() == LoginMethod.SSO)
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
