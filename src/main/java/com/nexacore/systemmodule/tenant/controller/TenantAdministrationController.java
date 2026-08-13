package com.nexacore.systemmodule.tenant.controller;

import com.nexacore.authmodule.security.service.TenantAccountUserDetails;
import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges;
import com.nexacore.systemmodule.tenant.dto.*;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.TenantAdministrationService;
import com.nexacore.systemmodule.tenant.security.StepUpAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedApi;
import com.nexacore.systemmodule.tenant.service.AuthorizedScopeLookupService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/system/tenants")
public class TenantAdministrationController {
    private final TenantAdministrationService service;
    private final StepUpAuthenticationService stepUpAuthenticationService;
    private final AuthorizedScopeLookupService scopeLookupService;

    @PostMapping("/authorized") @AuthenticatedApi
    public ResponseEntity<ApiResponse<List<ScopeLookupDto>>> authorized() {
        return ResponseEntity.ok(ApiResponse.success(scopeLookupService.tenants(),"Authorized tenants loaded"));
    }
    @PostMapping("/businesses") @AuthenticatedApi
    public ResponseEntity<ApiResponse<List<ScopeLookupDto>>> businesses(@RequestBody ScopeLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(scopeLookupService.businesses(request.tenantId()),"Authorized businesses loaded"));
    }
    @PostMapping("/branches") @AuthenticatedApi
    public ResponseEntity<ApiResponse<List<ScopeLookupDto>>> branches(@RequestBody ScopeLookupRequest request) {
        return ResponseEntity.ok(ApiResponse.success(scopeLookupService.branches(request.tenantId(),request.businessId()),"Authorized branches loaded"));
    }

    @PostMapping("/register")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_REGISTER)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_REGISTER)")
    public ResponseEntity<ApiResponse<TenantResponse>> register(@Valid @RequestBody TenantRequest request, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.register(request, actorId(authentication)), "Tenant registered in pending state"));
    }

    @PostMapping("/list")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_VIEW)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_VIEW)")
    public ResponseEntity<ApiResponse<List<TenantResponse>>> list(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(service.list(actorId(authentication)), "Tenants loaded"));
    }

    @PostMapping("/domain/verify")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_DOMAIN_VERIFY)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_DOMAIN_VERIFY)")
    public ResponseEntity<ApiResponse<TenantResponse>> verify(@Valid @RequestBody TenantDomainVerificationRequest request,
                                                               Authentication authentication, HttpServletRequest httpRequest) {
        stepUpAuthenticationService.requireRecentReauthentication(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(service.verifyDomain(request.domainId(), actorId(authentication)), "Tenant domain verified"));
    }

    @PostMapping("/activate")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_LIFECYCLE_MANAGE)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_LIFECYCLE_MANAGE)")
    public ResponseEntity<ApiResponse<TenantResponse>> activate(@Valid @RequestBody TenantLifecycleRequest request,
                                                                 Authentication authentication, HttpServletRequest httpRequest) {
        return transition(request, TenantStatus.ACTIVE, authentication, httpRequest, "Tenant activated");
    }

    @PostMapping("/suspend")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_LIFECYCLE_MANAGE)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_LIFECYCLE_MANAGE)")
    public ResponseEntity<ApiResponse<TenantResponse>> suspend(@Valid @RequestBody TenantLifecycleRequest request,
                                                                Authentication authentication, HttpServletRequest httpRequest) {
        return transition(request, TenantStatus.SUSPENDED, authentication, httpRequest, "Tenant suspended");
    }

    @PostMapping("/reactivate")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_LIFECYCLE_MANAGE)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_LIFECYCLE_MANAGE)")
    public ResponseEntity<ApiResponse<TenantResponse>> reactivate(@Valid @RequestBody TenantLifecycleRequest request,
                                                                   Authentication authentication, HttpServletRequest httpRequest) {
        return transition(request, TenantStatus.ACTIVE, authentication, httpRequest, "Tenant reactivated");
    }

    @PostMapping("/cancel")
    @PrivilegeApi(BootstrapAdministrationPrivileges.TENANT_LIFECYCLE_MANAGE)
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).TENANT_LIFECYCLE_MANAGE)")
    public ResponseEntity<ApiResponse<TenantResponse>> cancel(@Valid @RequestBody TenantLifecycleRequest request,
                                                               Authentication authentication, HttpServletRequest httpRequest) {
        return transition(request, TenantStatus.CANCELLED, authentication, httpRequest, "Tenant cancelled");
    }

    private ResponseEntity<ApiResponse<TenantResponse>> transition(TenantLifecycleRequest request, TenantStatus target,
                                                                    Authentication authentication, HttpServletRequest httpRequest,
                                                                    String message) {
        stepUpAuthenticationService.requireRecentReauthentication(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(service.transition(request, target, actorId(authentication)), message));
    }

    private long actorId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof TenantAccountUserDetails principal) return principal.accountId();
        throw new IllegalStateException("Tenant-bound authenticated principal is required");
    }
}
