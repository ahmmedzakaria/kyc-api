package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientScopeAssignmentDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplicationTenant;
import com.nexacore.systemmodule.accesscontrol.repository.*;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientPermissionServiceImplTest {
    private final ClientApplicationService clients = mock(ClientApplicationService.class);
    private final ApiRegistryRepository apis = mock(ApiRegistryRepository.class);
    private final PrivilegeRepository privileges = mock(PrivilegeRepository.class);
    private final ClientApiPermissionRepository apiPermissions = mock(ClientApiPermissionRepository.class);
    private final ClientFeaturePermissionRepository featurePermissions = mock(ClientFeaturePermissionRepository.class);
    private final ClientApplicationTenantRepository scopes = mock(ClientApplicationTenantRepository.class);
    private final AuthModuleGateway auth = mock(AuthModuleGateway.class);
    private final AuthorizationDataCache cache = mock(AuthorizationDataCache.class);
    private final ClientPermissionServiceImpl service = new ClientPermissionServiceImpl(
            clients, apis, privileges, apiPermissions, featurePermissions, scopes, auth, cache);

    @Test
    void replacesNormalizedHierarchicalScopeTuples() {
        SysAccClientApplication client = SysAccClientApplication.builder().id(9L).clientCode("WEB").build();
        when(clients.requireClientApplication(9L, null)).thenReturn(client);
        when(auth.getUserId("admin")).thenReturn(4L);
        when(scopes.findByClientApplicationIdAndActiveTrue(9L)).thenReturn(List.of());
        ClientPermissionAssignmentRequestDto request = new ClientPermissionAssignmentRequestDto();
        request.setClientApplicationId(9L);
        request.setScopeAssignments(Set.of(new ClientScopeAssignmentDto(7L, 8L, 10L)));

        service.assignTenants(request, "admin");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<SysAccClientApplicationTenant>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(scopes).deleteByClientApplicationId(9L);
        verify(scopes).saveAll(captor.capture());
        SysAccClientApplicationTenant saved = captor.getValue().iterator().next();
        assertThat(saved.getTenantId()).isEqualTo(7L);
        assertThat(saved.getBusinessId()).isEqualTo(8L);
        assertThat(saved.getBranchId()).isEqualTo(10L);
        verify(cache).invalidateClientAfterCommit(9L);
    }

    @Test
    void rejectsAmbiguousLegacyBusinessOnlyReplacement() {
        when(clients.requireClientApplication(9L, null))
                .thenReturn(SysAccClientApplication.builder().id(9L).build());
        ClientPermissionAssignmentRequestDto request = new ClientPermissionAssignmentRequestDto();
        request.setClientApplicationId(9L);
        request.setBusinessIds(Set.of(8L));
        assertThatThrownBy(() -> service.assignTenants(request, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scopeAssignments");
        verify(scopes, never()).deleteByClientApplicationId(anyLong());
    }
}
