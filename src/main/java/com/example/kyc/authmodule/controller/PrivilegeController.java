package com.example.kyc.authmodule.controller;

import com.example.kyc.authmodule.dto.ApplicationContextDto;
import com.example.kyc.authmodule.dto.PrivilegeAssignmentRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckRequestDto;
import com.example.kyc.authmodule.dto.PrivilegeCheckResponseDto;
import com.example.kyc.authmodule.dto.PrivilegeDto;
import com.example.kyc.authmodule.dto.PrivilegeFeatureDefinitionDto;
import com.example.kyc.authmodule.dto.PrivilegeRequestDto;
import com.example.kyc.authmodule.dto.SidebarMenuDto;
import com.example.kyc.authmodule.dto.SubMenuDto;
import com.example.kyc.authmodule.dto.SubMenuRequestDto;
import com.example.kyc.authmodule.service.interfaces.PrivilegeService;
import com.example.kyc.commonmodule.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/privilege")
public class PrivilegeController {

    private final PrivilegeService privilegeService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<PrivilegeDto>> savePrivilege(@RequestBody PrivilegeRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.savePrivilege(requestDto), "Privilege saved"));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<PrivilegeDto>>> listPrivileges() {
        return ResponseEntity.ok(ApiResponse.success(privilegeService.getAllPrivileges(), "Privileges loaded"));
    }

    @PostMapping("/definitions")
    public ResponseEntity<ApiResponse<List<PrivilegeFeatureDefinitionDto>>> listModulePrivilegeDefinitions() {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getModulePrivilegeDefinitions(),
                "Module privilege definitions loaded"
        ));
    }

    @PostMapping("/context")
    public ResponseEntity<ApiResponse<ApplicationContextDto>> applicationContext(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getApplicationContext(authentication.getName()),
                "Application context loaded"
        ));
    }

    @PostMapping("/sidebar-menu")
    public ResponseEntity<ApiResponse<List<SidebarMenuDto>>> sidebarMenu(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getUserSidebarMenu(authentication.getName()),
                "Sidebar menu loaded"
        ));
    }

    @PostMapping("/sub-menu/save")
    public ResponseEntity<ApiResponse<SubMenuDto>> saveSubMenu(@RequestBody SubMenuRequestDto requestDto,
                                                               Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.saveSubMenu(requestDto, authentication.getName()),
                "Sub menu saved"
        ));
    }

    @PostMapping("/sub-menu/list")
    public ResponseEntity<ApiResponse<List<SubMenuDto>>> listSubMenus() {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getAllSubMenus(),
                "Sub menus loaded"
        ));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<PrivilegeCheckResponseDto>> checkPrivilege(@RequestBody PrivilegeCheckRequestDto requestDto,
                                                                                 Authentication authentication) {
        if (requestDto.getUsername() == null || requestDto.getUsername().isBlank()) {
            requestDto.setUsername(authentication.getName());
        }
        return ResponseEntity.ok(ApiResponse.success(privilegeService.checkPrivilege(requestDto), "Privilege checked"));
    }

    @PostMapping("/my-codes")
    public ResponseEntity<ApiResponse<Set<String>>> myPrivilegeCodes(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                privilegeService.getUserPrivilegeCodes(authentication.getName()),
                "User privileges loaded"
        ));
    }

    @PostMapping("/assign-role")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToRole(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToRole(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "Role privileges updated"));
    }

    @PostMapping("/assign-user")
    public ResponseEntity<ApiResponse<Void>> assignPrivilegesToUser(@RequestBody PrivilegeAssignmentRequestDto requestDto) {
        privilegeService.assignPrivilegesToUser(requestDto);
        return ResponseEntity.ok(ApiResponse.success(null, "User privileges updated"));
    }
}
