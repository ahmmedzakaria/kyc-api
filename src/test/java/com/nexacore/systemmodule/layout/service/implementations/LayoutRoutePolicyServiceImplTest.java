package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.commonmodule.dto.RoutePrivilegePolicyDto;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicy;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicyPrivilege;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyPrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutRoutePolicyRepository;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LayoutRoutePolicyServiceImplTest {
    private final LayoutRoutePolicyRepository policyRepository = mock(LayoutRoutePolicyRepository.class);
    private final LayoutRoutePolicyPrivilegeRepository linkRepository = mock(LayoutRoutePolicyPrivilegeRepository.class);
    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final ClientApplicationRepository clientRepository = mock(ClientApplicationRepository.class);
    private final LayoutRoutePolicyServiceImpl service = new LayoutRoutePolicyServiceImpl(
            policyRepository, linkRepository, privilegeRepository, clientRepository
    );

    @Test
    void clientPolicyOverridesGlobalPolicyForSameRouteUrl() {
        SysLayoutRoutePolicy global = policy(1L, null, "/person", PrivilegeMatchMode.ALL);
        SysLayoutRoutePolicy client = policy(2L, 10L, "/person", PrivilegeMatchMode.ANY);
        SysPrivPrivilege view = SysPrivPrivilege.builder()
                .id(5L)
                .privilegeCode("01010200101")
                .active(true)
                .build();

        when(policyRepository.findEffectiveCandidates("WEB")).thenReturn(List.of(global, client));
        when(linkRepository.findByRoutePolicyIdAndActiveTrue(2L)).thenReturn(List.of(
                SysLayoutRoutePolicyPrivilege.builder().routePolicy(client).privilege(view).active(true).build()
        ));

        List<RoutePrivilegePolicyDto> policies = service.getEffectivePolicies("WEB");

        assertThat(policies).hasSize(1);
        assertThat(policies.getFirst().getRouteUrl()).isEqualTo("/person");
        assertThat(policies.getFirst().getMatchMode()).isEqualTo("ANY");
        assertThat(policies.getFirst().getPrivilegeCodes()).containsExactly("01010200101");
    }

    @Test
    void inactivePrivilegesAreNotPublishedInPolicyRequirements() {
        SysLayoutRoutePolicy policy = policy(1L, null, "/person", PrivilegeMatchMode.ANY);
        SysPrivPrivilege inactive = SysPrivPrivilege.builder()
                .id(5L)
                .privilegeCode("01010200101")
                .active(false)
                .build();
        when(policyRepository.findEffectiveCandidates("")).thenReturn(List.of(policy));
        when(linkRepository.findByRoutePolicyIdAndActiveTrue(1L)).thenReturn(List.of(
                SysLayoutRoutePolicyPrivilege.builder().routePolicy(policy).privilege(inactive).active(true).build()
        ));

        assertThat(service.getEffectivePolicies(null).getFirst().getPrivilegeCodes()).isEmpty();
    }

    private SysLayoutRoutePolicy policy(Long id, Long clientId, String routeUrl, PrivilegeMatchMode mode) {
        return SysLayoutRoutePolicy.builder()
                .id(id)
                .clientApplicationId(clientId)
                .routeUrl(routeUrl)
                .matchMode(mode)
                .active(true)
                .build();
    }
}
