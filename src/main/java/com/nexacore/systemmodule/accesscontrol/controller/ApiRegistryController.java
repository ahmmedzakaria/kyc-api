package com.nexacore.systemmodule.accesscontrol.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiInventoryItemDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncReportDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistrySyncApplyRequest;
import com.nexacore.systemmodule.accesscontrol.dto.CatalogPageRequest;
import com.nexacore.systemmodule.accesscontrol.dto.CatalogPageDto;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ApiInventoryService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
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
import com.nexacore.systemmodule.tenant.security.StepUpAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/api-registry")
public class ApiRegistryController {

    private final ClientApiRegistryService clientApiRegistryService;
    private final ApiInventoryService apiInventoryService;
    private final StepUpAuthenticationService stepUpAuthenticationService;

    @PostMapping("/inventory")
    @PrivilegeApi("11020100601")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<List<ApiInventoryItemDto>>> inventory() {
        return ResponseEntity.ok(ApiResponse.success(
                apiInventoryService.inventory(),
                "Application API inventory loaded"
        ));
    }

    @PostMapping("/save")
    @PrivilegeApi("11020100687")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_MANAGE)")
    public ResponseEntity<ApiResponse<ApiRegistryDto>> save(@RequestBody ApiRegistryRequestDto requestDto,
                                                            Authentication authentication,HttpServletRequest httpRequest) {
        stepUpAuthenticationService.requireRecentReauthentication(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.save(requestDto, authentication.getName()),
                "API registry saved"
        ));
    }

    @PostMapping("/list")
    @PrivilegeApi("11020100601")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<List<ApiRegistryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.list(),
                "API registry loaded"
        ));
    }

    @PostMapping("/search")
    @PrivilegeApi("11020100601")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<CatalogPageDto<ApiRegistryDto>>> search(@RequestBody CatalogPageRequest request) {
        String query=request.query()==null?"":request.query().trim().toLowerCase();
        List<ApiRegistryDto> filtered=clientApiRegistryService.list().stream().filter(item->query.isEmpty() ||
                item.getApiCode().toLowerCase().contains(query)||item.getPathPattern().toLowerCase().contains(query)).toList();
        return ResponseEntity.ok(ApiResponse.success(page(filtered,request),"API registry page loaded"));
    }

    @PostMapping("/inventory/search")
    @PrivilegeApi("11020100601")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<CatalogPageDto<ApiInventoryItemDto>>> inventorySearch(@RequestBody CatalogPageRequest request) {
        String query=request.query()==null?"":request.query().trim().toLowerCase();
        List<ApiInventoryItemDto> filtered=apiInventoryService.inventory().stream().filter(item->query.isEmpty() ||
                item.getApiCode().toLowerCase().contains(query)||item.getPathPattern().toLowerCase().contains(query)).toList();
        return ResponseEntity.ok(ApiResponse.success(page(filtered,request),"API inventory page loaded"));
    }

    private <T> CatalogPageDto<T> page(List<T> rows,CatalogPageRequest request) {
        int start=Math.min(request.page()*request.pageSize(),rows.size()); int end=Math.min(start+request.pageSize(),rows.size());
        return new CatalogPageDto<>(rows.subList(start,end),rows.size(),request.page(),request.pageSize());
    }

    @PostMapping("/sync")
    @PrivilegeApi("11020100680")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<ApiRegistrySyncReportDto>> sync(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.previewSyncFromAnnotations(),
                "API registry synchronization preview loaded; apply through /sync/apply"
        ));
    }

    @PostMapping("/sync/preview")
    @PrivilegeApi("11020100680")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<ApiRegistrySyncReportDto>> previewSync() {
        return ResponseEntity.ok(ApiResponse.success(clientApiRegistryService.previewSyncFromAnnotations(), "API registry synchronization preview loaded"));
    }

    @PostMapping("/sync/apply")
    @PrivilegeApi("11020100680")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<ApiRegistrySyncReportDto>> applySync(@RequestBody ApiRegistrySyncApplyRequest request,
                                                                            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.syncFromAnnotations(authentication.getName(), request.previewVersion()),
                "API registry synchronization applied"));
    }
}
