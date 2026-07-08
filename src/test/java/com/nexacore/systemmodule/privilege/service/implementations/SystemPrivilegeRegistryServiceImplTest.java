package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.systemmodule.privilege.entity.Privilege;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemPrivilegeRegistryServiceImplTest {

    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final RolePrivilegeRepository rolePrivilegeRepository = mock(RolePrivilegeRepository.class);
    private final SubMenuRepository subMenuRepository = mock(SubMenuRepository.class);
    private final SystemPrivilegeRegistryServiceImpl service = new SystemPrivilegeRegistryServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            subMenuRepository
    );

    @Test
    void delegatesPrivilegeQueriesToSystemRepository() {
        Privilege privilege = Privilege.builder()
                .privilegeCode("01010100101")
                .active(true)
                .build();
        Set<String> privilegeCodes = Set.of("01010100101");

        when(privilegeRepository.count()).thenReturn(1L);
        when(privilegeRepository.findByPrivilegeCode("01010100101")).thenReturn(Optional.of(privilege));
        when(privilegeRepository.findByPrivilegeCodeIn(privilegeCodes)).thenReturn(List.of(privilege));
        when(privilegeRepository.findAll()).thenReturn(List.of(privilege));

        assertThat(service.countPrivileges()).isEqualTo(1L);
        assertThat(service.findPrivilegeByCode("01010100101")).contains(privilege);
        assertThat(service.findPrivilegesByCodes(privilegeCodes)).containsExactly(privilege);
        assertThat(service.getAllPrivileges()).containsExactly(privilege);

        verify(privilegeRepository).count();
        verify(privilegeRepository).findByPrivilegeCode("01010100101");
        verify(privilegeRepository).findByPrivilegeCodeIn(privilegeCodes);
        verify(privilegeRepository).findAll();
    }

    @Test
    void delegatesSaveToSystemRepository() {
        Privilege privilege = Privilege.builder()
                .privilegeCode("01010100101")
                .active(true)
                .build();

        when(privilegeRepository.save(privilege)).thenReturn(privilege);

        assertThat(service.savePrivilege(privilege)).isSameAs(privilege);
        verify(privilegeRepository).save(privilege);
    }
}
