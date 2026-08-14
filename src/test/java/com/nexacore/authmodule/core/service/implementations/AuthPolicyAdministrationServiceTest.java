package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.AuthPolicyAdministrationRequest;
import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.gatewaymodule.client.service.interfaces.ClientTenantAssignmentGateway;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContext;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthPolicyAdministrationServiceTest {
    private final AuthClientAuthPolicyRepository repository = mock(AuthClientAuthPolicyRepository.class);
    private final ClientTenantAssignmentGateway assignmentGateway = mock(ClientTenantAssignmentGateway.class);
    private final AuthPolicyAdministrationService service = new AuthPolicyAdministrationService(repository, assignmentGateway);

    @AfterEach
    void clearContext() { AuthenticatedRequestContextHolder.clear(); }

    @Test
    void savesPolicyForAuthorizedTenantAndAssignedClient() {
        setContext(7L, 10L);
        when(repository.findByTenantIdAndClientCodeIgnoreCase(10L, "WEB")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
            AuthClientAuthPolicy policy = invocation.getArgument(0); policy.setId(4L); return policy;
        });

        var result = service.save(new AuthPolicyAdministrationRequest(
                10L, "web", LoginMethod.PASSWORD, LoginIdentifierType.EMAIL, true));

        assertThat(result.clientCode()).isEqualTo("WEB");
        assertThat(result.loginMethod()).isEqualTo(LoginMethod.PASSWORD);
        verify(assignmentGateway).requireActiveAssignment("web", 10L);
    }

    @Test
    void rejectsTenantOutsideAuthenticatedScope() {
        setContext(7L, 11L);
        assertThatThrownBy(() -> service.list(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tenant scope is not allowed");
    }

    @Test
    void rejectsMfaAsPrimaryMethod() {
        setContext(7L, 10L);
        assertThatThrownBy(() -> service.save(new AuthPolicyAdministrationRequest(
                10L, "WEB", LoginMethod.MFA, LoginIdentifierType.USERNAME, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Login method and identifier are incompatible");
    }

    private void setContext(Long userId, Long tenantId) {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(userId, "admin", 2L,
                "SYSTEM_ADMIN_WEB", Set.of(new UserScopeAssignment(tenantId, null, null)), "trace", Set.of()));
    }
}
