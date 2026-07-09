package com.nexacore.systemmodule.privilege.service.implementations;

import com.nexacore.authmodule.core.dto.SidebarMenuDto;
import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.privilege.entity.SysPrivilege;
import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import com.nexacore.systemmodule.privilege.enums.FeatureType;
import com.nexacore.systemmodule.privilege.repository.PrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.RolePrivilegeRepository;
import com.nexacore.systemmodule.privilege.repository.SubMenuRepository;
import com.nexacore.systemmodule.privilege.repository.UserPrivilegeRepository;
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
    private final AuthModuleGateway authModuleGateway = mock(AuthModuleGateway.class);
    private final SubMenuRepository subMenuRepository = mock(SubMenuRepository.class);
    private final PrivilegeServiceImpl service = new PrivilegeServiceImpl(
            privilegeRepository,
            rolePrivilegeRepository,
            userPrivilegeRepository,
            authModuleGateway,
            subMenuRepository,
            List.of()
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
        return SysSubMenu.builder()
                .id(id)
                .name(name)
                .url("/" + name.toLowerCase())
                .icon("fa fa-circle")
                .moduleCode("01")
                .moduleName("KYC")
                .submoduleCode("01")
                .submoduleName("Person")
                .featureTypeCode(FeatureType.OPERATIONS.getCode())
                .featureTypeName(FeatureType.OPERATIONS.getDisplayName())
                .featureCode(String.format("%03d", id))
                .featureName(name)
                .active(true)
                .menuOrder(menuOrder)
                .subMenuOrder(subMenuOrder)
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
