package com.nexacore.authmodule.core.service.interfaces;

import com.nexacore.authmodule.core.dto.RoleDto;
import com.nexacore.authmodule.core.dto.RoleRequestDto;
import com.nexacore.authmodule.core.dto.UserDto;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.dto.UserRoleAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentRequestDto;

import java.util.List;
import java.util.Set;

public interface UserAdminService {
    List<UserDto> listUsers();

    UserDto saveUser(UserRequestDto requestDto);

    void assignRoles(UserRoleAssignmentRequestDto requestDto);

    Set<UserScopeAssignmentDto> getScopeAssignments(Long userId);

    void replaceScopeAssignments(UserScopeAssignmentRequestDto requestDto);

    List<RoleDto> listRoles();

    RoleDto saveRole(RoleRequestDto requestDto);

    RoleDto saveGlobalRole(RoleRequestDto requestDto);
}
