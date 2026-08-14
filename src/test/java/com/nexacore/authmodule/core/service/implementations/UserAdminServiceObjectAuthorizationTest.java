package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.authmodule.core.service.UsernameNormalizer;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import com.nexacore.gatewaymodule.tenant.service.interfaces.TenantProvisioningGateway;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContext;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.tenant.service.AuthorizedScopeLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class UserAdminServiceObjectAuthorizationTest {
    @AfterEach void clearSecurity() { AuthenticatedRequestContextHolder.clear(); }

    @Test
    void directRoleReadFailsClosedOutsideEffectiveTenant() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        DataScopeService scope = mock(DataScopeService.class);
        when(scope.requireEffectiveTenant(null)).thenReturn(7L);
        when(roles.findByIdAndTenantId(99L, 7L)).thenReturn(Optional.empty());
        when(roles.findByIdAndTenantIdIsNull(99L)).thenReturn(Optional.empty());
        UserAdminServiceImpl service = new UserAdminServiceImpl(users, roles, mock(PersonModuleGateway.class),
                mock(PasswordEncoder.class), mock(UsernameNormalizer.class), scope, mock(AuthorizedScopeLookupService.class),
                mock(TenantProvisioningGateway.class));

        assertThatThrownBy(() -> service.getRole(99L)).isInstanceOf(DataScopeAccessDeniedException.class);
        verify(roles).findByIdAndTenantId(99L, 7L);
    }

    @Test
    void directUserReadUsesTenantPredicate() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        DataScopeService scope = mock(DataScopeService.class);
        when(scope.requireEffectiveTenant(null)).thenReturn(7L);
        when(users.findByIdAndTenantId(99L, 7L)).thenReturn(Optional.empty());
        UserAdminServiceImpl service = new UserAdminServiceImpl(users, roles, mock(PersonModuleGateway.class),
                mock(PasswordEncoder.class), mock(UsernameNormalizer.class), scope, mock(AuthorizedScopeLookupService.class),
                mock(TenantProvisioningGateway.class));

        assertThatThrownBy(() -> service.getUserRoleAssignments(99L)).isInstanceOf(DataScopeAccessDeniedException.class);
        verify(users).findByIdAndTenantId(99L, 7L);
    }

    @Test
    void platformAdministratorCreatesTenantAccountWithInitialTenantScope() {
        UserRepository users = mock(UserRepository.class);
        DataScopeService scope = mock(DataScopeService.class);
        PersonModuleGateway people = mock(PersonModuleGateway.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TenantProvisioningGateway tenants = mock(TenantProvisioningGateway.class);
        when(scope.requireEffectiveTenant(5L)).thenThrow(new DataScopeAccessDeniedException("outside scope"));
        when(users.findByTenantIdAndNormalizedUsername(5L, "tenant.admin")).thenReturn(Optional.empty());
        when(encoder.encode("secret")).thenReturn("encoded");
        PersonSummaryDto person = new PersonSummaryDto(); person.setId(22L);
        when(people.ensurePersonForUser(any(), any(), any(), any(), any())).thenReturn(person);
        when(users.save(any())).thenAnswer(invocation -> { AuthUser user = invocation.getArgument(0); user.setId(8L); return user; });
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(1L, "platform", 2L,
                "SYSTEM_ADMIN_WEB", Set.of(new UserScopeAssignment(1L, null, null)), "trace",
                Set.of(BootstrapAdministrationPrivileges.TENANT_VIEW)));
        UserAdminServiceImpl service = new UserAdminServiceImpl(users, mock(RoleRepository.class), people, encoder,
                new UsernameNormalizer(), scope, mock(AuthorizedScopeLookupService.class), tenants);
        UserRequestDto request = new UserRequestDto(); request.setTenantId(5L); request.setUsername("Tenant.Admin");
        request.setPassword("secret"); request.setEnabled(true);

        service.saveUser(request);

        verify(tenants).requireActiveTenant(5L);
        var captor = org.mockito.ArgumentCaptor.forClass(AuthUser.class); verify(users).save(captor.capture());
        assertThat(captor.getValue().getScopeAssignments()).singleElement()
                .satisfies(assignment -> assertThat(assignment.getTenantId()).isEqualTo(5L));
    }
}
