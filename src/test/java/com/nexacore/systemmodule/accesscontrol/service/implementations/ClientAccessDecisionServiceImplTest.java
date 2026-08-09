package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.servicesmodule.cacheservice.service.interfaces.CacheService;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAccessDecisionServiceImplTest {

    private final ClientApiPermissionRepository clientApiPermissionRepository = mock(ClientApiPermissionRepository.class);
    private final ClientFeaturePermissionRepository clientFeaturePermissionRepository = mock(ClientFeaturePermissionRepository.class);
    private final CacheService cacheService = mock(CacheService.class);
    private final AuthorizationDataCache authorizationDataCache = new AuthorizationDataCache(cacheService, mock(com.nexacore.systemmodule.accesscontrol.security.AccessControlMetrics.class));
    private final ClientAccessDecisionServiceImpl service = new ClientAccessDecisionServiceImpl(
            clientApiPermissionRepository,
            clientFeaturePermissionRepository,
            authorizationDataCache
    );

    { when(cacheService.get(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty()); }

    @Test
    void allowsPublicApiWithoutClient() {
        SysAccApiRegistry api = SysAccApiRegistry.builder()
                .id(1L)
                .publicApi(true)
                .active(true)
                .build();

        ClientAccessDecisionDto decision = service.decide(null, api);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void deniesRegisteredPrivateApiWithoutClient() {
        SysAccApiRegistry api = SysAccApiRegistry.builder()
                .id(1L)
                .publicApi(false)
                .active(true)
                .build();

        ClientAccessDecisionDto decision = service.decide(null, api);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denyReason()).isEqualTo("CLIENT_REQUIRED");
    }

    @Test
    void deniesWhenClientHasApiButMissingFeaturePermission() {
        SysAccClientApplication client = SysAccClientApplication.builder().id(10L).clientCode("WEB").build();
        SysAccApiRegistry api = SysAccApiRegistry.builder()
                .id(20L)
                .requiredPrivilegeCode("01010200101")
                .publicApi(false)
                .active(true)
                .build();

        when(clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(10L)).thenReturn(Set.of(20L));
        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(10L)).thenReturn(Set.of());

        ClientAccessDecisionDto decision = service.decide(client, api);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denyReason()).isEqualTo("CLIENT_FEATURE_NOT_ALLOWED");
    }

    @Test
    void deniesWhenClientDoesNotHaveApiGrant() {
        SysAccClientApplication client = SysAccClientApplication.builder().id(10L).clientCode("WEB").build();
        SysAccApiRegistry api = SysAccApiRegistry.builder().id(20L).publicApi(false).active(true).build();
        when(clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(10L)).thenReturn(Set.of());
        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(10L)).thenReturn(Set.of());

        ClientAccessDecisionDto decision = service.decide(client, api);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denyReason()).isEqualTo("CLIENT_API_NOT_ALLOWED");
    }

    @Test
    void allowsClientWithApiAndRequiredFeatureGrants() {
        SysAccClientApplication client = SysAccClientApplication.builder().id(10L).clientCode("WEB").build();
        SysAccApiRegistry api = SysAccApiRegistry.builder().id(20L)
                .requiredPrivilegeCode("01010200101").publicApi(false).active(true).build();
        when(clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(10L)).thenReturn(Set.of(20L));
        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(10L))
                .thenReturn(Set.of("01010200101"));

        assertThat(service.decide(client, api).allowed()).isTrue();
    }

    @Test
    void filtersUserPrivilegeCodesByClientFeaturePermissions() {
        SysAccClientApplication client = SysAccClientApplication.builder().id(10L).clientCode("WEB").build();
        Set<String> userPrivilegeCodes = Set.of("01010200101", "01010200201");

        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(10L))
                .thenReturn(Set.of("01010200201"));

        assertThat(service.filterPrivilegeCodesForClient(client, userPrivilegeCodes))
                .containsExactly("01010200201");
    }
}
