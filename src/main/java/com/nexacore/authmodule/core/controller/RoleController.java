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
import java.util.Set;
import com.nexacore.commonmodule.dto.IdRequestDto;
import com.nexacore.commonmodule.dto.VersionedAssignmentDto;
import com.nexacore.commonmodule.util.AssignmentVersion;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/role")
public class RoleController {

    private final UserAdminService userAdminService;
    private final PrivilegeService privilegeService;

    @PostMapping("/list")
    @PrivilegeApi("11020100801")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<RoleDto>>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.listRoles(), "Roles loaded"));
    }

    @PostMapping("/detail")
    @PrivilegeApi("11020100801")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<RoleDto>> roleDetail(@RequestBody IdRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.getRole(Long.valueOf(request.getId())), "Role loaded"));
    }

    @PostMapping("/privilege-assignments")
    @PrivilegeApi("11020100801")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_VIEW) and @privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_VIEW)")
    public ResponseEntity<ApiResponse<VersionedAssignmentDto<String>>> privilegeAssignments(@RequestBody IdRequestDto request) {
        Long roleId = Long.valueOf(request.getId());
        userAdminService.getRole(roleId);
        Set<String> codes = privilegeService.getRolePrivilegeCodes(roleId);
        return ResponseEntity.ok(ApiResponse.success(
                new VersionedAssignmentDto<>(AssignmentVersion.of(codes), codes.stream().sorted().toList()),
                "Role privilege assignments loaded"));
    }

    @PostMapping("/save")
    @PrivilegeApi("11020100887")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<RoleDto>> saveRole(@RequestBody RoleRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.saveRole(requestDto), "Role saved"));
    }

    @PostMapping("/global/save")
    @PrivilegeApi("11020100887")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and @privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).ROLE_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<RoleDto>> saveGlobalRole(@RequestBody RoleRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.saveGlobalRole(requestDto), "Global role template saved"));
    }
}
