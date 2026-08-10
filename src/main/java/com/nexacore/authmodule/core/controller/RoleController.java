package com.nexacore.authmodule.core.controller;

import com.nexacore.authmodule.core.dto.RoleDto;
import com.nexacore.authmodule.core.dto.RoleRequestDto;
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
@RequestMapping("/api/v1/system/role")
public class RoleController {

    private final UserAdminService userAdminService;

    @PostMapping("/list")
    @PrivilegeApi("11020100801")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.listRoles(), "Roles loaded"));
    }

    @PostMapping("/save")
    @PrivilegeApi("11020100887")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<RoleDto>> saveRole(@RequestBody RoleRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.saveRole(requestDto), "Role saved"));
    }
}
