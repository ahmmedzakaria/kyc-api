package com.nexacore.authmodule.core.controller;

import com.nexacore.authmodule.core.dto.UserDto;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.dto.UserRoleAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentDto;
import com.nexacore.authmodule.core.dto.UserScopeAssignmentRequestDto;
import com.nexacore.authmodule.core.service.interfaces.UserAdminService;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import com.nexacore.commonmodule.dto.IdRequestDto;
import com.nexacore.commonmodule.dto.VersionedAssignmentDto;
import com.nexacore.authmodule.core.dto.RoleDto;
import com.nexacore.authmodule.core.dto.UserListRequestDto;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/user")
public class UserController {

    private final UserAdminService userAdminService;

    @PostMapping("/list")
    @PrivilegeApi("11020100701")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<UserDto>>> listUsers(@RequestBody(required = false) UserListRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.listUsers(request == null ? null : request.tenantId()), "Users loaded"));
    }

    @PostMapping("/detail")
    @PrivilegeApi("11020100701")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<UserDto>> userDetail(@RequestBody IdRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.getUser(Long.valueOf(request.getId())), "User loaded"));
    }

    @PostMapping("/role-assignments")
    @PrivilegeApi("11020100701")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<VersionedAssignmentDto<RoleDto>>> roleAssignments(@RequestBody IdRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(
                userAdminService.getUserRoleAssignments(Long.valueOf(request.getId())), "User role assignments loaded"));
    }

    @PostMapping("/save")
    @PrivilegeApi("11020100787")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<UserDto>> saveUser(@RequestBody UserRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.saveUser(requestDto), "User saved"));
    }

    @PostMapping("/assign-roles")
    @PrivilegeApi("11020100781")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignRoles(@RequestBody UserRoleAssignmentRequestDto requestDto) {
        userAdminService.assignRoles(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "User roles updated"));
    }

    @PostMapping("/scope-assignments")
    @PrivilegeApi("11020100701")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<VersionedAssignmentDto<UserScopeAssignmentDto>>> scopeAssignments(
            @RequestBody UserScopeAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                userAdminService.getScopeAssignments(requestDto.getUserId()), "User scope assignments loaded"));
    }

    @PostMapping("/replace-scope-assignments")
    @PrivilegeApi("11020100781")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> replaceScopeAssignments(
            @RequestBody UserScopeAssignmentRequestDto requestDto) {
        userAdminService.replaceScopeAssignments(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "User scope assignments updated"));
    }
}
