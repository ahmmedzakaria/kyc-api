package com.nexacore.authmodule.service.interfaces;

import com.nexacore.authmodule.dto.ApplicationContextDto;
import com.nexacore.authmodule.dto.PrivilegeAssignmentRequestDto;
import com.nexacore.authmodule.dto.PrivilegeCheckRequestDto;
import com.nexacore.authmodule.dto.PrivilegeCheckResponseDto;
import com.nexacore.authmodule.dto.PrivilegeDto;
import com.nexacore.authmodule.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.authmodule.dto.PrivilegeRequestDto;
import com.nexacore.authmodule.dto.SidebarMenuDto;
import com.nexacore.authmodule.dto.SubMenuDto;
import com.nexacore.authmodule.dto.SubMenuRequestDto;

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
