package com.nexacore.systemmodule.layout.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentDto;
import com.nexacore.systemmodule.layout.dto.ClientLayoutAssignmentRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutContextDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationCategoryOrderRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutNavigationNodeRequestDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileDto;
import com.nexacore.systemmodule.layout.dto.LayoutProfileRequestDto;
import com.nexacore.systemmodule.layout.dto.NavNodeDto;
import com.nexacore.systemmodule.layout.service.interfaces.ClientLayoutAssignmentService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutContextService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutNavigationService;
import com.nexacore.systemmodule.layout.service.interfaces.LayoutProfileService;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import lombok.Data;
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
@RequestMapping("/api/v1/system/layout")
public class LayoutController {
    private final LayoutProfileService layoutProfileService;
    private final ClientLayoutAssignmentService clientLayoutAssignmentService;
    private final LayoutContextService layoutContextService;
    private final LayoutNavigationService layoutNavigationService;
    private final PrivilegeService privilegeService;

    @PostMapping("/profile/save")
    public ResponseEntity<ApiResponse<LayoutProfileDto>> saveProfile(@RequestBody LayoutProfileRequestDto request,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutProfileService.save(request, authentication.getName()),
                "Layout profile saved"
        ));
    }

    @PostMapping("/profile/list")
    public ResponseEntity<ApiResponse<List<LayoutProfileDto>>> listProfiles() {
        return ResponseEntity.ok(ApiResponse.success(layoutProfileService.list(), "Layout profiles loaded"));
    }

    @PostMapping("/profile/detail")
    public ResponseEntity<ApiResponse<LayoutProfileDto>> profileDetail(@RequestBody LayoutProfileDetailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                layoutProfileService.detail(request.getProfileCode()),
                "Layout profile loaded"
        ));
    }

    @PostMapping("/client/assign")
    public ResponseEntity<ApiResponse<ClientLayoutAssignmentDto>> assignClientProfile(@RequestBody ClientLayoutAssignmentRequestDto request,
                                                                                     Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientLayoutAssignmentService.assign(request, authentication.getName()),
                "Client layout assignment saved"
        ));
    }

    @PostMapping("/client/list")
    public ResponseEntity<ApiResponse<List<ClientLayoutAssignmentDto>>> listClientProfiles(@RequestBody ClientLayoutListRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                clientLayoutAssignmentService.list(request.getClientCode()),
                "Client layout assignments loaded"
        ));
    }

    @PostMapping("/context")
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
    public ResponseEntity<ApiResponse<NavNodeDto>> saveGroup(@RequestBody LayoutNavigationNodeRequestDto request,
                                                             Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveGroup(request, authentication.getName()), "Layout group saved"));
    }

    @PostMapping("/navigation/module/save")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveModule(@RequestBody LayoutNavigationNodeRequestDto request,
                                                              Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveModule(request, authentication.getName()), "Layout module saved"));
    }

    @PostMapping("/navigation/category/save")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveCategory(@RequestBody LayoutNavigationNodeRequestDto request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveCategory(request, authentication.getName()), "Layout category saved"));
    }

    @PostMapping("/navigation/category/list")
    public ResponseEntity<ApiResponse<List<NavNodeDto>>> listCategories() {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.listCategories(), "Layout categories loaded"));
    }

    @PostMapping("/navigation/category/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderCategories(@RequestBody LayoutNavigationCategoryOrderRequestDto request,
                                                               Authentication authentication) {
        layoutNavigationService.reorderCategories(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Layout categories reordered"));
    }

    @PostMapping("/navigation/feature-group/save")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveFeatureGroup(@RequestBody LayoutNavigationNodeRequestDto request,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveFeatureGroup(request, authentication.getName()), "Layout feature group saved"));
    }

    @PostMapping("/navigation/feature/save")
    public ResponseEntity<ApiResponse<NavNodeDto>> saveFeature(@RequestBody LayoutNavigationNodeRequestDto request,
                                                               Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(layoutNavigationService.saveFeature(request, authentication.getName()), "Layout feature saved"));
    }

    @PostMapping("/navigation/tree")
    public ResponseEntity<ApiResponse<List<NavNodeDto>>> navigationTree(@RequestBody LayoutContextRequest request,
                                                                        Authentication authentication) {
        String username = authentication == null ? request.getUsername() : authentication.getName();
        Set<String> privilegeCodes = username == null ? Set.of() : privilegeService.getUserPrivilegeCodes(username);
        return ResponseEntity.ok(ApiResponse.success(
                layoutNavigationService.getNavigationTree(request.getClientCode(), username, privilegeCodes),
                "Layout navigation tree loaded"
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
