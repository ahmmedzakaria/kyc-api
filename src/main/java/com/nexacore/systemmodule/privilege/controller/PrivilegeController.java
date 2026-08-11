package com.nexacore.systemmodule.accesscontrol.controller;

import com.nexacore.authmodule.core.dto.ApplicationContextDto;
import com.nexacore.authmodule.core.dto.PrivilegeAssignmentRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckRequestDto;
import com.nexacore.authmodule.core.dto.PrivilegeCheckResponseDto;
import com.nexacore.authmodule.core.dto.PrivilegeDto;
import com.nexacore.authmodule.core.dto.PrivilegeRequestDto;
import com.nexacore.authmodule.core.dto.SidebarMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuDto;
import com.nexacore.authmodule.core.dto.SubMenuRequestDto;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedApi;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/privilege")
public class PrivilegeController {

    private final PrivilegeService privilegeService;

    @PostMapping("/save")
    @PrivilegeApi("11010100180")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<PrivilegeDto>> savePrivilege(@RequestBody PrivilegeRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.savePrivilege(requestDto), "Privilege saved"));
    }

    @PostMapping("/list")
    @PrivilegeApi("11010100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_VIEW)")
    public ResponseEntity<ApiResponse<List<PrivilegeDto>>> listPrivileges() {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.getAllPrivileges(), "Privileges loaded"));
    }

    @PostMapping("/definitions")
    @PrivilegeApi("11010100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_VIEW)")
    public ResponseEntity<ApiResponse<List<PrivilegeFeatureDefinitionDto>>> listModulePrivilegeDefinitions() {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getModulePrivilegeDefinitions(),
                "Module privilege definitions loaded"
        ));
    }

    @PostMapping("/context")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<ApplicationContextDto>> applicationContext(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getApplicationContext(authentication.getName()),
                "Application context loaded"
        ));
    }

    @PostMapping("/sidebar-menu")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<List<SidebarMenuDto>>> sidebarMenu(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getUserSidebarMenu(authentication.getName()),
                "Sidebar menu loaded"
        ));
    }

    @PostMapping("/sub-menu/save")
    @PrivilegeApi("11010100180")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<SubMenuDto>> saveSubMenu(@RequestBody SubMenuRequestDto requestDto,
                                                               Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.saveSubMenu(requestDto, authentication.getName()),
                "Sub menu saved"
        ));
    }

    @PostMapping("/sub-menu/list")
    @PrivilegeApi("11010100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_VIEW)")
    public ResponseEntity<ApiResponse<List<SubMenuDto>>> listSubMenus() {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getAllSubMenus(),
                "Sub menus loaded"
        ));
    }

    @PostMapping("/check")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<PrivilegeCheckResponseDto>> checkPrivilege(@RequestBody PrivilegeCheckRequestDto requestDto,
                                                                                 Authentication authentication) {
        if (requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
            requestDto.setUsername(authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.success(privilegeService.checkPrivilege(requestDto), "Privilege checked"));
    }

    @PostMapping("/my-codes")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<Set<String>>> myPrivilegeCodes(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getUserPrivilegeCodes(authentication.getName()),
                "User privileges loaded"
        ));
    }

    @PostMapping("/role-codes")
    @PrivilegeApi("11010100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_VIEW)")
    public ResponseEntity<ApiResponse<Set<String>>> rolePrivilegeCodes(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getRolePrivilegeCodes(requestDto.getRoleId()),
                "Role privileges loaded"
        ));
    }

    @PostMapping("/assign-role")
    @PrivilegeApi("11010100181")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToRole(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToRole(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "Role privileges updated"));
    }

    @PostMapping("/assign-user")
    @PrivilegeApi("11010100181")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).PRIVILEGE_CATALOG_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToUser(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToUser(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "User privileges updated"));
    }
}
