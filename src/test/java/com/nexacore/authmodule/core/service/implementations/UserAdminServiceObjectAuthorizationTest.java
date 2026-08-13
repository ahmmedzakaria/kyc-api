package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.authmodule.core.service.UsernameNormalizer;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.tenant.service.AuthorizedScopeLookupService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAdminServiceObjectAuthorizationTest {
    @Test
    void directRoleReadFailsClosedOutsideEffectiveTenant() {
        UserRepository users = mock(UserRepository.class);
        RoleRepository roles = mock(RoleRepository.class);
        DataScopeService scope = mock(DataScopeService.class);
        when(scope.requireEffectiveTenant(null)).thenReturn(7L);
        when(roles.findByIdAndTenantId(99L, 7L)).thenReturn(Optional.empty());
        when(roles.findByIdAndTenantIdIsNull(99L)).thenReturn(Optional.empty());
        UserAdminServiceImpl service = new UserAdminServiceImpl(users, roles, mock(PersonModuleGateway.class),
                mock(PasswordEncoder.class), mock(UsernameNormalizer.class), scope, mock(AuthorizedScopeLookupService.class));

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
                mock(PasswordEncoder.class), mock(UsernameNormalizer.class), scope, mock(AuthorizedScopeLookupService.class));

        assertThatThrownBy(() -> service.getUserRoleAssignments(99L)).isInstanceOf(DataScopeAccessDeniedException.class);
        verify(users).findByIdAndTenantId(99L, 7L);
    }
}
