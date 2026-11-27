package com.nexacore.systemmodule.publicnavigation.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.gatewaymodule.navigation.dto.PublicNavigationContextDto;
import com.nexacore.gatewaymodule.navigation.enums.PublicNavigationSurface;
import com.nexacore.gatewaymodule.navigation.service.interfaces.PublicNavigationModuleGateway;
import com.nexacore.systemmodule.accesscontrol.security.ApiDataScope;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import com.nexacore.systemmodule.accesscontrol.security.PublicApi;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.publicnavigation.dto.*;
import com.nexacore.systemmodule.publicnavigation.service.PublicNavigationAdministrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/public-navigation")
public class PublicNavigationController {
    private final PublicNavigationAdministrationService administrationService;
    private final PublicNavigationModuleGateway gateway;

    @PostMapping("/public/context")
    @PublicApi
    public ResponseEntity<ApiResponse<PublicNavigationContextDto>> publicContext(@RequestBody PublicContextRequest request) {
        Set<PublicNavigationSurface> surfaces = request.surfaces() == null || request.surfaces().isEmpty()
                ? EnumSet.allOf(PublicNavigationSurface.class)
                : request.surfaces().stream().map(value -> PublicNavigationSurface.valueOf(value.toUpperCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(PublicNavigationSurface.class)));
        return ResponseEntity.ok(ApiResponse.success(gateway.getPublicNavigation(request.clientCode(), surfaces),
                "Public navigation loaded"));
    }

    @PostMapping("/groups/list")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<PublicNavigationGroupDto>>> groups(@Valid @RequestBody PublicNavigationScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.listGroups(request), "Public navigation groups loaded"));
    }

    @PostMapping("/groups/save")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<PublicNavigationGroupDto>> saveGroup(@Valid @RequestBody PublicNavigationGroupRequest request,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.saveGroup(request, authentication.getName()),
                "Public navigation group saved"));
    }

    @PostMapping("/items/list")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<List<PublicNavigationItemDto>>> items(@Valid @RequestBody PublicNavigationScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.listItems(request), "Public navigation items loaded"));
    }

    @PostMapping("/items/save")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<PublicNavigationItemDto>> saveItem(@Valid @RequestBody PublicNavigationItemRequest request,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.saveItem(request, authentication.getName()),
                "Public navigation item saved"));
    }

    @PostMapping("/items/deactivate")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<PublicNavigationItemDto>> deactivate(@Valid @RequestBody ItemMutationRequest request,
                                                                           Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.deactivateItem(
                request.tenantId(), request.itemId(), request.version(), authentication.getName()),
                "Public navigation item deactivated"));
    }

    @PostMapping("/items/delete")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<Void>> delete(@Valid @RequestBody ItemMutationRequest request) {
        administrationService.deleteItem(request.tenantId(), request.itemId());
        return ResponseEntity.ok(ApiResponse.success(null, "Public navigation item deleted"));
    }

    @PostMapping("/assignments/list")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<PublicNavigationAssignmentDto>> assignments(@Valid @RequestBody PublicNavigationScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.listAssignments(request),
                "Public navigation assignments loaded"));
    }

    @PostMapping("/assignments/replace")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_MANAGE)")
    public ResponseEntity<ApiResponse<PublicNavigationAssignmentDto>> replace(@Valid @RequestBody PublicNavigationAssignmentRequest request,
                                                                              Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.replaceAssignments(request, authentication.getName()),
                "Public navigation assignments replaced"));
    }

    @PostMapping("/integrity")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<PublicNavigationIntegrityDto>> integrity(@Valid @RequestBody PublicNavigationScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.integrity(request.tenantId()),
                "Public navigation integrity loaded"));
    }

    @PostMapping("/preview")
    @PrivilegeApi(value = BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW, dataScope = ApiDataScope.TENANT)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LAYOUT_ADMINISTRATION_VIEW)")
    public ResponseEntity<ApiResponse<PublicNavigationContextDto>> preview(@Valid @RequestBody PublicNavigationScopeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(administrationService.preview(request), "Public navigation preview loaded"));
    }

    public record PublicContextRequest(String clientCode, Set<String> surfaces) {}
    public record ItemMutationRequest(Long tenantId, Long itemId, Long version) {}

}
