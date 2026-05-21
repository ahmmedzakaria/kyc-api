package com.nexacore.authmodule.core.service.interfaces;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.PrivilegeAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckResponseDto;
import com.nexacore.authmodule.core.dto.PrivilegeDto;
import com.nexacore.authmodule.core.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.authmodule.core.dto.PrivilegeRequestDto;
import com.nexacore.authmodule.core.dto.SidebarMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuRequestDto;

import java.util.List;
import java.util.Set;

public interface PrivilegeService {
    String buildPrivilegeCode(String moduleCode, String featureTypeCode, String featureCode, String actionCode);

    PrivilegeDto savePrivilege(PrivilegeRequestDto requestDto);

    List<PrivilegeDto> getAllPrivileges();

    List<PrivilegeFeatureDefinitionDto> getModulePrivilegeDefinitions();

    SubMenuDto saveSubMenu(SubMenuRequestDto requestDto, String username);

    List<SubMenuDto> getAllSubMenus();

    ApplicationContextDto getApplicationContext(String username);

    List<SidebarMenuDto> getUserSidebarMenu(String username);

    Set<String> getUserPrivilegeCodes(String username);

    PrivilegeCheckResponseDto checkPrivilege(PrivilegeCheckRequestDto requestDto);

    void assignPrivilegesToRole(PrivilegeAssignmentRequestDto requestDto);

    void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto);
}
