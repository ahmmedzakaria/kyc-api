package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.ResolvedAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.security.config.KeycloakProperties;
import com.nexacore.authmodule.security.config.RegistrationProperties;
import com.nexacore.authmodule.security.service.TenantAccountResolver;
import com.nexacore.gatewaymodule.client.dto.ClientSsoConfigurationDto;
import com.nexacore.gatewaymodule.client.service.interfaces.ClientConfigurationGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthApplicationContextServiceTest {

    @SuppressWarnings("removal")
    @Test
    void projectsOneCanonicalPolicyIntoCompatibilityCollections() {
        AuthClientPolicyService policyService = mock(AuthClientPolicyService.class);
        TenantAccountResolver tenantResolver = mock(TenantAccountResolver.class);
        ClientConfigurationGateway clientConfigurationGateway = mock(ClientConfigurationGateway.class);
        when(tenantResolver.resolveRequiredTenant("SYSTEM_ADMIN_WEB")).thenReturn(10L);
        when(policyService.resolveRequiredPolicy(10L, "SYSTEM_ADMIN_WEB")).thenReturn(new ResolvedAuthPolicy(
                10L, "SYSTEM_ADMIN_WEB", LoginMethod.SSO, LoginIdentifierType.USERNAME,
                RegistrationCredentialModel.ENTERPRISE_SSO, "7:2026-08-14T12:00"));
        when(clientConfigurationGateway.resolveSsoConfiguration("SYSTEM_ADMIN_WEB", "http://localhost:5301"))
                .thenReturn(new ClientSsoConfigurationDto("SYSTEM_ADMIN_WEB", "system-admin-oauth",
                        "http://localhost:5301/sso/callback", "http://localhost:5301/login"));

        var service = new AuthApplicationContextService(
                new AuthenticationProperties(),
                new RegistrationProperties(),
                new KeycloakProperties(),
                policyService,
                tenantResolver,
                clientConfigurationGateway
        );

        var context = service.buildPublicContext("http://localhost:5301", "SYSTEM_ADMIN_WEB");

        assertThat(context.getTenantId()).isEqualTo(10L);
        assertThat(context.getClientCode()).isEqualTo("SYSTEM_ADMIN_WEB");
        assertThat(context.getLoginMethod()).isEqualTo(LoginMethod.SSO);
        assertThat(context.getLoginIdentifierType()).isEqualTo(LoginIdentifierType.USERNAME);
        assertThat(context.getRegistrationCredentialModel()).isEqualTo(RegistrationCredentialModel.ENTERPRISE_SSO);
        assertThat(context.getSecondFactorPolicy().mode()).isEqualTo("DISABLED");
        assertThat(context.getEnabledLoginMethods()).containsExactly(LoginMethod.SSO);
        assertThat(context.getLoginIdentifierTypes()).containsExactly(LoginIdentifierType.USERNAME);
        assertThat(context.getEnabledRegistrationCredentialModels())
                .containsExactly(RegistrationCredentialModel.ENTERPRISE_SSO);
        assertThat(context.getSecurityPolicy().ssoLoginEnabled()).isTrue();
        assertThat(context.getSecurityPolicy().passwordLoginEnabled()).isFalse();
        assertThat(context.getSso().clientId()).isEqualTo("system-admin-oauth");
        assertThat(context.getSso().redirectUri()).isEqualTo("http://localhost:5301/sso/callback");
    }
}
