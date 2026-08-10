package com.nexacore.authmodule.core.controller;

import com.nexacore.authmodule.core.dto.UserDto;
import com.nexacore.authmodule.core.dto.UserRequestDto;
import com.nexacore.authmodule.core.dto.UserRoleAssignmentRequestDto;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/user")
public class UserController {

    private final UserAdminService userAdminService;

    @PostMapping("/list")
    @PrivilegeApi("11020100701")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).USER_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<UserDto>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.listUsers(), "Users loaded"));
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
}
