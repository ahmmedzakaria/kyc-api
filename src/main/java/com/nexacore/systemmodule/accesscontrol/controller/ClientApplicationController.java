package com.nexacore.systemmodule.accesscontrol.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientAdministrationDetailDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientScopeAssignmentDto;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientPermissionService;
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
@RequestMapping("/api/v1/system/client-app")
public class ClientApplicationController {

    private final ClientApplicationService clientApplicationService;
    private final ClientPermissionService clientPermissionService;

    @PostMapping("/save")
    @PrivilegeApi("11020100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_MANAGE)")
    public ResponseEntity<ApiResponse<ClientApplicationDto>> save(@RequestBody ClientApplicationRequestDto requestDto,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.save(requestDto, authentication.getName()),
                "Client application saved"
        ));
    }

    @PostMapping("/list")
    @PrivilegeApi("11020100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_VIEW)")
    public ResponseEntity<ApiResponse<List<ClientApplicationDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.list(),
                "Client applications loaded"
        ));
    }

    @PostMapping("/detail")
    @PrivilegeApi("11020100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_VIEW)")
    public ResponseEntity<ApiResponse<ClientAdministrationDetailDto>> detail(@RequestBody ClientPermissionAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                clientPermissionService.getAdministrationDetail(requestDto.getClientApplicationId(), requestDto.getClientCode()),
                "Client administration detail loaded"));
    }

    @PostMapping("/api-permissions")
    @PrivilegeApi("11020100381")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_API_PERMISSION_ASSIGN)")
    public ResponseEntity<ApiResponse<Set<Long>>> apiPermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                clientPermissionService.getApiPermissions(requestDto.getClientApplicationId(), requestDto.getClientCode()),
                "Client API permissions loaded"));
    }

    @PostMapping("/feature-permissions")
    @PrivilegeApi("11020100481")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_FEATURE_PERMISSION_ASSIGN)")
    public ResponseEntity<ApiResponse<Set<String>>> featurePermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                clientPermissionService.getFeaturePermissions(requestDto.getClientApplicationId(), requestDto.getClientCode()),
                "Client feature permissions loaded"));
    }

    @PostMapping("/scope-assignments")
    @PrivilegeApi("11020100581")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_TENANT_ASSIGN)")
    public ResponseEntity<ApiResponse<Set<ClientScopeAssignmentDto>>> scopeAssignments(@RequestBody ClientPermissionAssignmentRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(
                clientPermissionService.getScopeAssignments(requestDto.getClientApplicationId(), requestDto.getClientCode()),
                "Client scope assignments loaded"));
    }

    @PostMapping("/rotate-api-key")
    @PrivilegeApi("11020100283")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_CREDENTIAL_ROTATE)")
    public ResponseEntity<ApiResponse<GeneratedClientCredentialDto>> rotateApiKey(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.rotateApiKey(requestDto.getClientApplicationId(), requestDto.getClientCode(), authentication.getName()),
                "Client API key rotated"
        ));
    }

    @PostMapping("/assign-api-permissions")
    @PrivilegeApi("11020100381")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_API_PERMISSION_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignApiPermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                  Authentication authentication) {
        clientPermissionService.assignApiPermissions(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client API permissions updated"));
    }

    @PostMapping("/assign-feature-permissions")
    @PrivilegeApi("11020100481")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_FEATURE_PERMISSION_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignFeaturePermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                      Authentication authentication) {
        clientPermissionService.assignFeaturePermissions(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client feature permissions updated"));
    }

    @PostMapping("/assign-tenants")
    @PrivilegeApi("11020100581")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_TENANT_ASSIGN)")
    public ResponseEntity<ApiResponse<Void>> assignTenants(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                           Authentication authentication) {
        if (requestDto.getVersion()==null || requestDto.getVersion().isBlank())
            throw new IllegalArgumentException("Assignment version is required; reload before replacing scopes");
        clientPermissionService.assignTenants(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client tenants updated"));
    }
}
