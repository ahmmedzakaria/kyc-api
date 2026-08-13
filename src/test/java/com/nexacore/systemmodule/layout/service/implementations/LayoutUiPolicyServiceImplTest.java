package com.nexacore.systemmodule.layout.service.implementations;

import com.nexacore.commonmodule.dto.UiPrivilegePolicyDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicy;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicyPrivilege;
import com.nexacore.systemmodule.layout.enums.PrivilegeMatchMode;
import com.nexacore.systemmodule.layout.dto.LayoutUiPolicyRequestDto;
import com.nexacore.systemmodule.layout.repository.LayoutUiPolicyPrivilegeRepository;
import com.nexacore.systemmodule.layout.repository.LayoutUiPolicyRepository;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class LayoutUiPolicyServiceImplTest {
    private final LayoutUiPolicyRepository policyRepository = mock(LayoutUiPolicyRepository.class);
    private final LayoutUiPolicyPrivilegeRepository linkRepository = mock(LayoutUiPolicyPrivilegeRepository.class);
    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final ClientApplicationRepository clientRepository = mock(ClientApplicationRepository.class);
    private final AuthModuleGateway authModuleGateway = mock(AuthModuleGateway.class);
    private final LayoutUiPolicyServiceImpl service = new LayoutUiPolicyServiceImpl(
            policyRepository, linkRepository, privilegeRepository, clientRepository, authModuleGateway
    );

    @Test
    void clientPolicyOverridesGlobalPolicyForSameActionCode() {
        SysLayoutUiPolicy global = policy(1L, null, PrivilegeMatchMode.ALL);
        SysLayoutUiPolicy client = policy(2L, 10L, PrivilegeMatchMode.ANY);
        SysPrivPrivilege update = SysPrivPrivilege.builder()
                .id(5L).privilegeCode("01010200112").active(true).build();
        when(policyRepository.findEffectiveCandidates("WEB")).thenReturn(List.of(global, client));
        when(linkRepository.findByUiPolicyIdAndActiveTrue(2L)).thenReturn(List.of(
                SysLayoutUiPolicyPrivilege.builder().uiPolicy(client).privilege(update).active(true).build()
        ));

        List<UiPrivilegePolicyDto> policies = service.getEffectivePolicies("WEB");

        assertThat(policies).hasSize(1);
        assertThat(policies.getFirst().getActionCode()).isEqualTo("person.list.edit-button");
        assertThat(policies.getFirst().getMatchMode()).isEqualTo("ANY");
        assertThat(policies.getFirst().getPrivilegeCodes()).containsExactly("01010200112");
    }

    @Test
    void inactivePrivilegesAreNotPublished() {
        SysLayoutUiPolicy policy = policy(1L, null, PrivilegeMatchMode.ANY);
        SysPrivPrivilege inactive = SysPrivPrivilege.builder()
                .id(5L).privilegeCode("01010200112").active(false).build();
        when(policyRepository.findEffectiveCandidates("")).thenReturn(List.of(policy));
        when(linkRepository.findByUiPolicyIdAndActiveTrue(1L)).thenReturn(List.of(
                SysLayoutUiPolicyPrivilege.builder().uiPolicy(policy).privilege(inactive).active(true).build()
        ));

        assertThat(service.getEffectivePolicies(null).getFirst().getPrivilegeCodes()).isEmpty();
    }

    @Test
    void saveUsesVersionedFullReplacementAndAllowsRemovalOfEveryPrivilege() {
        SysLayoutUiPolicy existing = policy(1L, null, PrivilegeMatchMode.ANY);
        existing.setVersion(4L);
        when(authModuleGateway.getUserId("admin")).thenReturn(9L);
        when(policyRepository.findByClientApplicationIdAndActionCode(null, "layout.manage"))
                .thenReturn(Optional.of(existing));
        when(policyRepository.save(existing)).thenReturn(existing);
        when(linkRepository.findByUiPolicyIdAndActiveTrue(1L)).thenReturn(List.of());
        LayoutUiPolicyRequestDto request = new LayoutUiPolicyRequestDto();
        request.setActionCode("layout.manage");
        request.setMatchMode(PrivilegeMatchMode.ANY);
        request.setVersion(4L);
        request.setPrivilegeCodes(Set.of());

        UiPrivilegePolicyDto saved = service.save(request, "admin");

        verify(linkRepository).deleteByUiPolicyId(1L);
        verify(linkRepository).saveAll(List.of());
        assertThat(saved.getPrivilegeCodes()).isEmpty();
        assertThat(saved.getVersion()).isEqualTo(4L);
    }

    private SysLayoutUiPolicy policy(Long id, Long clientId, PrivilegeMatchMode mode) {
        return SysLayoutUiPolicy.builder()
                .id(id)
                .clientApplicationId(clientId)
                .actionCode("person.list.edit-button")
                .matchMode(mode)
                .active(true)
                .build();
    }
}
