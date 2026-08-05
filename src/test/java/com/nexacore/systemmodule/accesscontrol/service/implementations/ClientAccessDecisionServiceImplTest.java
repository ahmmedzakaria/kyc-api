package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAccessDecisionServiceImplTest {

    private final ClientApiPermissionRepository clientApiPermissionRepository = mock(ClientApiPermissionRepository.class);
    private final ClientFeaturePermissionRepository clientFeaturePermissionRepository = mock(ClientFeaturePermissionRepository.class);
    private final ClientAccessDecisionServiceImpl service = new ClientAccessDecisionServiceImpl(
            clientApiPermissionRepository,
            clientFeaturePermissionRepository
    );

    @Test
    void allowsPublicApiWithoutClient() {
        SysPrivApiRegistry api = SysPrivApiRegistry.builder()
                .id(1L)
                .publicApi(true)
                .active(true)
                .build();

        ClientAccessDecisionDto decision = service.decide(null, api);

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void deniesRegisteredPrivateApiWithoutClient() {
        SysPrivApiRegistry api = SysPrivApiRegistry.builder()
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
        SysPrivClientApplication client = SysPrivClientApplication.builder().id(10L).clientCode("WEB").build();
        SysPrivApiRegistry api = SysPrivApiRegistry.builder()
                .id(20L)
                .requiredPrivilegeCode("01010200101")
                .publicApi(false)
                .active(true)
                .build();

        when(clientApiPermissionRepository.existsByClientApplicationIdAndApiRegistryIdAndActiveTrue(10L, 20L)).thenReturn(true);
        when(clientFeaturePermissionRepository.existsByClientApplicationIdAndPrivilegePrivilegeCodeAndActiveTrue(10L, "01010200101"))
                .thenReturn(false);

        ClientAccessDecisionDto decision = service.decide(client, api);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.denyReason()).isEqualTo("CLIENT_FEATURE_NOT_ALLOWED");
    }

    @Test
    void filtersUserPrivilegeCodesByClientFeaturePermissions() {
        SysPrivClientApplication client = SysPrivClientApplication.builder().id(10L).clientCode("WEB").build();
        Set<String> userPrivilegeCodes = Set.of("01010200101", "01010200201");

        when(clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(10L))
                .thenReturn(Set.of("01010200201"));

        assertThat(service.filterPrivilegeCodesForClient(client, userPrivilegeCodes))
                .containsExactly("01010200201");
    }
}
