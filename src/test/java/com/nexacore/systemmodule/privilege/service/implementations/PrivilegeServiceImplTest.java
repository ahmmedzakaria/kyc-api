package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.authmodule.core.service.implementations.AuthApplicationContextService;
import com.nexacore.authmodule.core.dto.SidebarMenuDto;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientApplicationContextService;
import com.nexacore.systemmodule.privilege.catalog.entity.SysFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.catalog.entity.SysSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.SubmoduleRepository;
import com.nexacore.systemmodule.privilege.assignment.repository.UserPrivilegeRepository;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrivilegeServiceImplTest {

    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final RolePrivilegeRepository rolePrivilegeRepository = mock(RolePrivilegeRepository.class);
    private final UserPrivilegeRepository userPrivilegeRepository = mock(UserPrivilegeRepository.class);
    private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    private final SubmoduleRepository submoduleRepository = mock(SubmoduleRepository.class);
    private final FeatureRepository featureRepository = mock(FeatureRepository.class);
    private final AuthModuleGateway authModuleGateway = mock(AuthModuleGateway.class);
    private final SubMenuRepository subMenuRepository = mock(SubMenuRepository.class);
    private final ClientApplicationContextService clientApplicationContextService = mock(ClientApplicationContextService.class);
    private final AuthApplicationContextService authApplicationContextService = mock(AuthApplicationContextService.class);
    private final LayoutContextService layoutContextService = mock(LayoutContextService.class);
    private final PrivilegeServiceImpl service = new PrivilegeServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            userPrivilegeRepository,
            moduleRepository,
            submoduleRepository,
            featureRepository,
            authModuleGateway,
            subMenuRepository,
            List.of(),
            clientApplicationContextService,
            authApplicationContextService,
            layoutContextService
    );

    @Test
    void sortsSidebarChildrenByConfiguredSubMenuOrder() {
        SysSubMenu laterMenu = subMenu(1L, "Later", 20, 30);
        SysSubMenu earlierMenu = subMenu(2L, "Earlier", 20, 10);

        SysPrivilege laterPrivilege = privilege("01010200101", laterMenu);
        SysPrivilege earlierPrivilege = privilege("01010200201", earlierMenu);
        Set<String> privilegeCodes = Set.of("01010200101", "01010200201");

        when(authModuleGateway.getUserAccess("operator")).thenReturn(AuthUserAccessDto.builder()
                .userId(10L)
                .roleIds(Set.of())
                .admin(false)
                .build());
        when(privilegeRepository.findActivePrivilegeCodesByUserId(10L)).thenReturn(List.copyOf(privilegeCodes));
        when(privilegeRepository.findByPrivilegeCodeIn(privilegeCodes)).thenReturn(List.of(laterPrivilege, earlierPrivilege));
        when(clientApplicationContextService.getCurrentClientPrivilegeCodes(privilegeCodes)).thenReturn(privilegeCodes);

        List<SidebarMenuDto> menus = service.getUserSidebarMenu("operator");

        SidebarMenuDto operationsMenu = menus.stream()
                .filter(menu -> FeatureType.OPERATIONS.getDisplayName().equals(menu.getLabel()))
                .findFirst()
                .orElseThrow();

        assertThat(operationsMenu.getMenuOrder()).isEqualTo(20);
        assertThat(operationsMenu.getChildren())
                .extracting(SidebarMenuDto::getLabel)
                .containsExactly("Earlier", "Later");
        assertThat(operationsMenu.getChildren())
                .extracting(SidebarMenuDto::getSubMenuOrder)
                .containsExactly(10, 30);
    }

    private SysSubMenu subMenu(Long id, String name, Integer menuOrder, Integer subMenuOrder) {
        SysFeature feature = feature(id, name);
        return SysSubMenu.builder()
                .id(id)
                .name(name)
                .url("/" + name.toLowerCase())
                .icon("fa fa-circle")
                .feature(feature)
                .active(true)
                .menuOrder(menuOrder)
                .subMenuOrder(subMenuOrder)
                .build();
    }

    private SysFeature feature(Long id, String name) {
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
                .id(id)
                .submodule(submodule)
                .featureTypeCode(FeatureType.OPERATIONS.getCode())
                .featureTypeName(FeatureType.OPERATIONS.getDisplayName())
                .code(String.format("%03d", id))
                .name(name)
                .active(true)
                .build();
    }

    private SysPrivilege privilege(String privilegeCode, SysSubMenu subMenu) {
        return SysPrivilege.builder()
                .privilegeCode(privilegeCode)
                .moduleCode(subMenu.getModuleCode())
                .moduleName(subMenu.getModuleName())
                .submoduleCode(subMenu.getSubmoduleCode())
                .submoduleName(subMenu.getSubmoduleName())
                .featureTypeCode(subMenu.getFeatureTypeCode())
                .featureTypeName(subMenu.getFeatureTypeName())
                .featureCode(subMenu.getFeatureCode())
                .featureName(subMenu.getFeatureName())
                .actionCode("01")
                .actionName("Create")
                .subMenu(subMenu)
                .active(true)
                .build();
    }
}
