package com.nexacore.systemmodule.privilege.catalog.service.implementations;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeatureType;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivAction;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureTypeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ActionRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SystemPrivilegeRegistryServiceImplTest {

    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final RolePrivilegeRepository rolePrivilegeRepository = mock(RolePrivilegeRepository.class);
    private final SubMenuRepository subMenuRepository = mock(SubMenuRepository.class);
    private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    private final SubmoduleRepository submoduleRepository = mock(SubmoduleRepository.class);
    private final FeatureRepository featureRepository = mock(FeatureRepository.class);
    private final FeatureTypeRepository featureTypeRepository = mock(FeatureTypeRepository.class);
    private final ActionRepository actionRepository = mock(ActionRepository.class);
    private final SystemPrivilegeRegistryServiceImpl service = new SystemPrivilegeRegistryServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            subMenuRepository,
            moduleRepository,
            submoduleRepository,
            featureRepository,
            featureTypeRepository,
            actionRepository
    );

    @Test
    void syncsApplicationModulesAndSubmodulesToCatalogTables() {
        Map<String, SysPrivModule> modules = new HashMap<>();
        Map<String, SysPrivSubmodule> submodules = new HashMap<>();

        when(moduleRepository.findByCode(any())).thenAnswer(invocation ->
                Optional.ofNullable(modules.get(invocation.getArgument(0, String.class))));
        when(moduleRepository.save(any(SysPrivModule.class))).thenAnswer(invocation -> {
            SysPrivModule module = invocation.getArgument(0, SysPrivModule.class);
            modules.put(module.getCode(), module);
            return module;
        });
        when(submoduleRepository.findByModuleCodeAndCode(any(), any())).thenAnswer(invocation -> {
            String moduleCode = invocation.getArgument(0, String.class);
            String submoduleCode = invocation.getArgument(1, String.class);
            return Optional.ofNullable(submodules.get(moduleCode + ":" + submoduleCode));
        });
        when(submoduleRepository.save(any(SysPrivSubmodule.class))).thenAnswer(invocation -> {
            SysPrivSubmodule submodule = invocation.getArgument(0, SysPrivSubmodule.class);
            submodules.put(submodule.getModule().getCode() + ":" + submodule.getCode(), submodule);
            return submodule;
        });

        service.syncApplicationCatalog();

        ArgumentCaptor<SysPrivModule> moduleCaptor = ArgumentCaptor.forClass(SysPrivModule.class);
        ArgumentCaptor<SysPrivSubmodule> submoduleCaptor = ArgumentCaptor.forClass(SysPrivSubmodule.class);
        verify(moduleRepository, times(ApplicationModule.values().length)).save(moduleCaptor.capture());
        verify(submoduleRepository, times(ApplicationSubmodule.values().length)).save(submoduleCaptor.capture());

        assertThat(moduleCaptor.getAllValues())
                .extracting(SysPrivModule::getCode)
                .containsExactlyInAnyOrderElementsOf(List.of(ApplicationModule.values()).stream()
                        .map(ApplicationModule::getCode)
                        .toList());

        assertThat(submoduleCaptor.getAllValues().stream()
                .map(submodule -> submodule.getModule().getCode() + ":" + submodule.getCode())
                .collect(Collectors.toSet()))
                .containsExactlyInAnyOrderElementsOf(List.of(ApplicationSubmodule.values()).stream()
                        .map(submodule -> submodule.getModule().getCode() + ":" + submodule.getCode())
                        .collect(Collectors.toSet()));
    }

    @Test
    void delegatesPrivilegeQueriesToSystemRepository() {
        SysPrivPrivilege privilege = SysPrivPrivilege.builder()
                .privilegeCode("01010100101")
                .feature(feature())
                .action(action())
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
        SysPrivPrivilege privilege = SysPrivPrivilege.builder()
                .privilegeCode("01010100101")
                .feature(feature())
                .action(action())
                .active(true)
                .build();

        when(privilegeRepository.save(privilege)).thenReturn(privilege);

        assertThat(service.savePrivilege(privilege)).isSameAs(privilege);
        verify(privilegeRepository).save(privilege);
    }

    private SysPrivFeature feature() {
        SysPrivModule module = SysPrivModule.builder()
                .id(1L)
                .code("01")
                .name("KYC")
                .active(true)
                .build();
        SysPrivSubmodule submodule = SysPrivSubmodule.builder()
                .id(1L)
                .module(module)
                .code("01")
                .name("Person")
                .active(true)
                .build();
        return SysPrivFeature.builder()
                .id(1L)
                .submodule(submodule)
                .featureType(SysPrivFeatureType.builder().id(1L).featureTypeCode("01").featureTypeName("Setup").active(true).build())
                .featureCode("001")
                .featureName("Person")
                .active(true)
                .build();
    }

    private SysPrivAction action() {
        return SysPrivAction.builder().id(1L).actionCode("01").actionName("View").active(true).build();
    }
}
