package com.nexacore.systemmodule.layout.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.commonmodule.dto.UiPrivilegePolicyDto;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentDto;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutContextDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationCategoryOrderRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationNodeRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;
import com.nexacore.systemmodule.layout.dto.NavNodeDto;
import com.nexacore.systemmodule.layout.dto.LayoutUiPolicyRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutTenantReconciliationDto;
import com.nexacore.systemmodule.layout.service.interfaces.ClientLayoutAssignmentService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutProfileService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutUiPolicyService;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedApi;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.Data;
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
@RequestMapping("/api/v1/system/layout")
public class LayoutController {
    private final LayoutProfileService layoutProfileService;
    private final ClientLayoutAssignmentService clientLayoutAssignmentService;
    private final LayoutContextService layoutContextService;
    private final LayoutNavigationService layoutNavigationService;
    private final PrivilegeService privilegeService;
    private final LayoutUiPolicyService layoutUiPolicyService;

    @PostMapping("/ui-policy/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<UiPrivilegePolicyDto>> saveUiPolicy(@RequestBody LayoutUiPolicyRequestDto request,
                                                                          Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutUiPolicyService.save(request, authentication.getName()),
                "UI policy saved"
        ));
    }

    @PostMapping("/ui-policy/list")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<UiPrivilegePolicyDto>>> listUiPolicies(@RequestBody LayoutContextRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutUiPolicyService.getEffectivePolicies(request.getClientCode()),
                "UI policies loaded"
        ));
    }

    @PostMapping("/profile/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<LayoutProfileDto>> saveProfile(@RequestBody LayoutProfileRequestDto request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutProfileService.save(request, authentication.getName()),
                "Layout profile saved"
        ));
    }

    @PostMapping("/profile/list")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<LayoutProfileDto>>> listProfiles() {
        return ResponseEntity.ok(ApiResponse.success(layoutProfileService.list(), "Layout profiles loaded"));
    }

    @PostMapping("/profile/detail")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<LayoutProfileDto>> profileDetail(@RequestBody LayoutProfileDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutProfileService.detail(request.getProfileCode()),
                "Layout profile loaded"
        ));
    }

    @PostMapping("/client/assign")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<ClientLayoutAssignmentDto>> assignClientProfile(@RequestBody ClientLayoutAssignmentRequestDto request,
                                                                                     Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientLayoutAssignmentService.assign(request, authentication.getName()),
                "Client layout assignment saved"
        ));
    }

    @PostMapping("/client/list")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<ClientLayoutAssignmentDto>>> listClientProfiles(@RequestBody ClientLayoutListRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientLayoutAssignmentService.list(request.getClientCode()),
                "Client layout assignments loaded"
        ));
    }

    @PostMapping("/client/reconcile")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<LayoutTenantReconciliationDto>> reconcileClientProfiles() {
        return ResponseEntity.ok(ApiResponse.success(
                clientLayoutAssignmentService.reconcileCurrentTenant(),
                "Tenant layout assignments reconciled"
        ));
    }

    @PostMapping("/context")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<LayoutContextDto>> context(@RequestBody LayoutContextRequest request,
                                                                 Authentication authentication) {
        String username = authentication == null ? request.getUsername() : authentication.getName();
        Set<String> privilegeCodes = username == null ? Set.of() : privilegeService.getUserPrivilegeCodes(username);
        return ResponseEntity.ok(ApiResponse.success(
                layoutContextService.getEffectiveLayout(request.getClientCode(), username, privilegeCodes),
                "Layout context loaded"
        ));
    }

    @PostMapping("/navigation/group/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveGroup(@RequestBody LayoutNavigationNodeRequestDto request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveGroup(request, authentication.getName()), "Layout group saved"));
    }

    @PostMapping("/navigation/module/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveModule(@RequestBody LayoutNavigationNodeRequestDto request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveModule(request, authentication.getName()), "Layout module saved"));
    }

    @PostMapping("/navigation/category/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveCategory(@RequestBody LayoutNavigationNodeRequestDto request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveCategory(request, authentication.getName()), "Layout category saved"));
    }

    @PostMapping("/navigation/category/list")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<NavNodeDto>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.listCategories(), "Layout categories loaded"));
    }

    @PostMapping("/navigation/category/reorder")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<Void>> reorderCategories(@RequestBody LayoutNavigationCategoryOrderRequestDto request,
                                                               Authentication authentication) {
        layoutNavigationService.reorderCategories(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Layout categories reordered"));
    }

    @PostMapping("/navigation/feature-group/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveFeatureGroup(@RequestBody LayoutNavigationNodeRequestDto request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveFeatureGroup(request, authentication.getName()), "Layout feature group saved"));
    }

    @PostMapping("/navigation/feature/save")
    @PrivilegeApi("11040100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveFeature(@RequestBody LayoutNavigationNodeRequestDto request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveFeature(request, authentication.getName()), "Layout feature saved"));
    }

    @PostMapping("/navigation/tree")
    @AuthenticatedApi
    public ResponseEntity<ApiResponse<List<NavNodeDto>>> navigationTree(@RequestBody LayoutContextRequest request,
                                                                        Authentication authentication) {
        String username = authentication == null ? request.getUsername() : authentication.getName();
        Set<String> privilegeCodes = username == null ? Set.of() : privilegeService.getUserPrivilegeCodes(username);
        return ResponseEntity.ok(ApiResponse.success(
                layoutNavigationService.getNavigationTree(request.getClientCode(), username, privilegeCodes),
                "Layout navigation tree loaded"
        ));
    }

    // @Todo Need to Validate with "/navigation/tree"
    @PostMapping("/navigation/tree/admin")
    @PrivilegeApi("11040100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<NavNodeDto>>> navigationTreeAdmin() {
        return ResponseEntity.ok(ApiResponse.success(
                layoutNavigationService.getFullNavigationTree(),
                "Layout navigation tree (admin) loaded"
        ));
    }

    @Data
    public static class LayoutProfileDetailRequest {
        private String profileCode;
    }

    @Data
    public static class ClientLayoutListRequest {
        private String clientCode;
    }

    @Data
    public static class LayoutContextRequest {
        private String clientCode;
        private String username;
    }
}
