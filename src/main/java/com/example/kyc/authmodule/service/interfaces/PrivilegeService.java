package com.example.kyc.authmodule.service.interfaces;

import com.example.kyc.authmodule.dto.PrivilegeAssignmentRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckResponseDto;
import com.example.kyc.authmodule.dto.PrivilegeDto;
import com.example.kyc.authmodule.dto.PrivilegeFeatureDefinitionDto;
import com.example.kyc.authmodule.dto.PrivilegeRequestDto;
import com.example.kyc.authmodule.dto.SidebarMenuDto;

import java.util.List;
import java.util.Set;

public interface PrivilegeService {
    String buildPrivilegeCode(String moduleCode, String featureTypeCode, String featureCode, String actionCode);

    PrivilegeDto savePrivilege(PrivilegeRequestDto requestDto);

    List<PrivilegeDto> getAllPrivileges();

    List<PrivilegeFeatureDefinitionDto> getModulePrivilegeDefinitions();

    List<SidebarMenuDto> getUserSidebarMenu(String username);

    Set<String> getUserPrivilegeCodes(String username);

    PrivilegeCheckResponseDto checkPrivilege(PrivilegeCheckRequestDto requestDto);

    void assignPrivilegesToRole(PrivilegeAssignmentRequestDto requestDto);

    void assignPrivilegesToUser(PrivilegeAssignmentRequestDto requestDto);
}
