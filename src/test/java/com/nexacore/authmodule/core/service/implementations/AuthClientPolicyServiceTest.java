package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.exception.AuthPolicyException;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthClientPolicyServiceTest {

    private final AuthClientAuthPolicyRepository authPolicyRepository = mock(AuthClientAuthPolicyRepository.class);
    private final AuthClientPolicyService service = new AuthClientPolicyService(authPolicyRepository);

    @Test
    void resolvesOneCanonicalTenantClientPolicy() {
        when(authPolicyRepository.findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(10L, "SYSTEM_ADMIN_WEB"))
                .thenReturn(Optional.of(policy(1L, 10L, "SYSTEM_ADMIN_WEB", LoginMethod.SSO,
                        LoginIdentifierType.USERNAME)));

        var resolved = service.resolveRequiredPolicy(10L, " SYSTEM_ADMIN_WEB ");

        assertThat(resolved.tenantId()).isEqualTo(10L);
        assertThat(resolved.clientCode()).isEqualTo("SYSTEM_ADMIN_WEB");
        assertThat(resolved.loginMethod()).isEqualTo(LoginMethod.SSO);
        assertThat(resolved.loginIdentifierType()).isEqualTo(LoginIdentifierType.USERNAME);
        assertThat(resolved.registrationCredentialModel()).isEqualTo(RegistrationCredentialModel.ENTERPRISE_SSO);
        assertThat(resolved.policyVersion()).startsWith("1:");
    }

    @Test
    void includesTenantInRepositoryPredicate() {
        when(authPolicyRepository.findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(20L, "WEB"))
                .thenReturn(Optional.of(policy(2L, 20L, "WEB", LoginMethod.PASSWORD,
                        LoginIdentifierType.USERNAME)));

        service.resolveRequiredPolicy(20L, "WEB");

        verify(authPolicyRepository).findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(20L, "WEB");
    }

    @Test
    void missingOrUnresolvedPolicyFailsClosedWithoutPropertyFallback() {
        when(authPolicyRepository.findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(10L, "WEB"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveRequiredPolicy(10L, "WEB"))
                .isInstanceOf(AuthPolicyException.class)
                .extracting("code")
                .isEqualTo("AUTH_POLICY_NOT_CONFIGURED");
    }

    @Test
    void requestedMethodMustMatchCanonicalMethod() {
        var policy = new com.nexacore.authmodule.core.dto.ResolvedAuthPolicy(
                10L, "WEB", LoginMethod.PASSWORD, LoginIdentifierType.USERNAME,
                RegistrationCredentialModel.EMAIL_PASSWORD, "1:0");

        assertThatThrownBy(() -> service.requireLoginMethod(policy, LoginMethod.SSO))
                .isInstanceOf(AuthPolicyException.class)
                .extracting("code")
                .isEqualTo("LOGIN_METHOD_NOT_ALLOWED");
    }

    @Test
    void mfaCannotBeConfiguredAsPrimaryMethod() {
        when(authPolicyRepository.findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(10L, "WEB"))
                .thenReturn(Optional.of(policy(3L, 10L, "WEB", LoginMethod.MFA,
                        LoginIdentifierType.USERNAME)));

        assertThatThrownBy(() -> service.resolveRequiredPolicy(10L, "WEB"))
                .isInstanceOf(AuthPolicyException.class)
                .extracting("code")
                .isEqualTo("AUTH_POLICY_INTEGRITY_ERROR");
    }

    private AuthClientAuthPolicy policy(Long id, Long tenantId, String clientCode,
                                        LoginMethod method, LoginIdentifierType identifier) {
        return AuthClientAuthPolicy.builder()
                .id(id)
                .tenantId(tenantId)
                .clientCode(clientCode)
                .loginMethod(method)
                .loginIdentifierType(identifier)
                .enabled(true)
                .updatedAt(LocalDateTime.of(2026, 8, 14, 12, 0))
                .build();
    }
}
