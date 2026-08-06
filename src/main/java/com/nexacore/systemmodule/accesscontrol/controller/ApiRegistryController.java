package com.nexacore.systemmodule.accesscontrol.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiRegistryRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.ApiInventoryItemDto;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ApiInventoryService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/api-registry")
public class ApiRegistryController {

    private final ClientApiRegistryService clientApiRegistryService;
    private final ApiInventoryService apiInventoryService;

    @PostMapping("/inventory")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<List<ApiInventoryItemDto>>> inventory() {
        return ResponseEntity.ok(ApiResponse.success(
                apiInventoryService.inventory(),
                "Application API inventory loaded"
        ));
    }

    @PostMapping("/save")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_MANAGE)")
    public ResponseEntity<ApiResponse<ApiRegistryDto>> save(@RequestBody ApiRegistryRequestDto requestDto,
                                                            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.save(requestDto, authentication.getName()),
                "API registry saved"
        ));
    }

    @PostMapping("/list")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_VIEW)")
    public ResponseEntity<ApiResponse<List<ApiRegistryDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.list(),
                "API registry loaded"
        ));
    }

    @PostMapping("/sync")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).API_REGISTRY_SYNCHRONIZE)")
    public ResponseEntity<ApiResponse<List<ApiRegistryDto>>> sync(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApiRegistryService.syncFromAnnotations(authentication.getName()),
                "API registry synchronized"
        ));
    }
}
