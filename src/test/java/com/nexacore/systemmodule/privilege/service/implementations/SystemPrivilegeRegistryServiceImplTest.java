package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.systemmodule.privilege.entity.SysFeature;
import com.nexacore.systemmodule.privilege.entity.SysModule;
import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysSubmodule;
import com.nexacore.systemmodule.privilege.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.repository.SubmoduleRepository;
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
    private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    private final SubmoduleRepository submoduleRepository = mock(SubmoduleRepository.class);
    private final FeatureRepository featureRepository = mock(FeatureRepository.class);
    private final SystemPrivilegeRegistryServiceImpl service = new SystemPrivilegeRegistryServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            subMenuRepository,
            moduleRepository,
            submoduleRepository,
            featureRepository
    );

    @Test
    void delegatesPrivilegeQueriesToSystemRepository() {
        SysPrivilege privilege = SysPrivilege.builder()
                .privilegeCode("01010100101")
                .feature(feature())
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
        SysPrivilege privilege = SysPrivilege.builder()
                .privilegeCode("01010100101")
                .feature(feature())
                .active(true)
                .build();

        when(privilegeRepository.save(privilege)).thenReturn(privilege);

        assertThat(service.savePrivilege(privilege)).isSameAs(privilege);
        verify(privilegeRepository).save(privilege);
    }

    private SysFeature feature() {
        SysModule module = SysModule.builder()
                .id(1L)
                .code("01")
                .name("KYC")
                .active(true)
                .build();
        SysSubmodule submodule = SysSubmodule.builder()
                .id(1L)
                .module(module)
                .code("01")
                .name("Person")
                .active(true)
                .build();
        return SysFeature.builder()
                .id(1L)
                .submodule(submodule)
                .featureTypeCode("01")
                .featureTypeName("Setup")
                .code("001")
                .name("Person")
                .active(true)
                .build();
    }
}
