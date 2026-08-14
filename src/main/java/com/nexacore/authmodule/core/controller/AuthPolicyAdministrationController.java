package com.nexacore.authmodule.core.controller;

import com.nexacore.authmodule.core.dto.AuthPolicyAdministrationDto;
import com.nexacore.authmodule.core.dto.AuthPolicyAdministrationRequest;
import com.nexacore.authmodule.core.service.implementations.AuthPolicyAdministrationService;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/auth-policy")
public class AuthPolicyAdministrationController {
    private final AuthPolicyAdministrationService service;

    @PostMapping("/list")
    @PrivilegeApi("11020100101")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_VIEW)")
    public ResponseEntity<ApiResponse<List<AuthPolicyAdministrationDto>>> list(@RequestBody AuthPolicyAdministrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.list(request.tenantId()), "Authentication policies loaded"));
    }

    @PostMapping("/save")
    @PrivilegeApi("11020100187")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).CLIENT_APPLICATION_MANAGE)")
    public ResponseEntity<ApiResponse<AuthPolicyAdministrationDto>> save(@RequestBody AuthPolicyAdministrationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.save(request), "Authentication policy saved"));
    }
}
