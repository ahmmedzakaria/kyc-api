package com.nexacore.systemmodule.clientaccess.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationDto;
import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.clientaccess.dto.ClientPermissionAssignmentRequestDto;
import com.nexacore.systemmodule.clientaccess.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system/client-app")
public class ClientApplicationController {

    private final ClientApplicationService clientApplicationService;
    private final ClientPermissionService clientPermissionService;

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<ClientApplicationDto>> save(@RequestBody ClientApplicationRequestDto requestDto,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.save(requestDto, authentication.getName()),
                "Client application saved"
        ));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<List<ClientApplicationDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.list(),
                "Client applications loaded"
        ));
    }

    @PostMapping("/rotate-api-key")
    public ResponseEntity<ApiResponse<GeneratedClientCredentialDto>> rotateApiKey(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                                  Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                clientApplicationService.rotateApiKey(requestDto.getClientApplicationId(), requestDto.getClientCode(), authentication.getName()),
                "Client API key rotated"
        ));
    }

    @PostMapping("/assign-api-permissions")
    public ResponseEntity<ApiResponse<Void>> assignApiPermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                  Authentication authentication) {
        clientPermissionService.assignApiPermissions(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client API permissions updated"));
    }

    @PostMapping("/assign-feature-permissions")
    public ResponseEntity<ApiResponse<Void>> assignFeaturePermissions(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                                      Authentication authentication) {
        clientPermissionService.assignFeaturePermissions(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client feature permissions updated"));
    }

    @PostMapping("/assign-tenants")
    public ResponseEntity<ApiResponse<Void>> assignTenants(@RequestBody ClientPermissionAssignmentRequestDto requestDto,
                                                           Authentication authentication) {
        clientPermissionService.assignTenants(requestDto, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(null, "Client tenants updated"));
    }
}
