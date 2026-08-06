package com.nexacore.systemmodule.license.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;
import com.nexacore.systemmodule.license.dto.GeneratedLicenseKeyDto;
import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseKeyRequestDto;
import com.nexacore.systemmodule.license.dto.LicensePlanRequestDto;
import com.nexacore.systemmodule.license.dto.LicensePlanResponseDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionResponseDto;
import com.nexacore.systemmodule.license.service.interfaces.LicenseDecisionService;
import com.nexacore.systemmodule.license.service.interfaces.LicenseKeyService;
import com.nexacore.systemmodule.license.service.interfaces.LicensePlanService;
import com.nexacore.systemmodule.license.service.interfaces.LicenseSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicensePlanService licensePlanService;
    private final LicenseSubscriptionService licenseSubscriptionService;
    private final LicenseKeyService licenseKeyService;
    private final LicenseDecisionService licenseDecisionService;

    @PostMapping("/plan/save")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<LicensePlanResponseDto> savePlan(@RequestBody LicensePlanRequestDto request) {
        return ApiResponse.successCode(
                licensePlanService.savePlan(request),
                "system.license.plan.saved",
                "License plan saved"
        );
    }

    @PostMapping("/plan/list")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_VIEW)")
    public ApiResponse<List<LicensePlanResponseDto>> listPlans() {
        return ApiResponse.successCode(
                licensePlanService.listPlans(),
                "system.license.plan.listed",
                "License plans listed"
        );
    }

    @PostMapping("/plan/entitlement/save")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<Void> savePlanEntitlement(@RequestBody LicenseEntitlementRequestDto request) {
        licensePlanService.savePlanEntitlement(request);
        return ApiResponse.successCode("system.license.plan.entitlement.saved", "License plan entitlement saved");
    }

    @PostMapping("/subscription/assign")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<LicenseSubscriptionResponseDto> assignSubscription(@RequestBody LicenseSubscriptionRequestDto request) {
        return ApiResponse.successCode(
                licenseSubscriptionService.assignSubscription(request),
                "system.license.subscription.assigned",
                "License subscription assigned"
        );
    }

    @PostMapping("/subscription/list")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_VIEW)")
    public ApiResponse<List<LicenseSubscriptionResponseDto>> listSubscriptions() {
        return ApiResponse.successCode(
                licenseSubscriptionService.listSubscriptions(),
                "system.license.subscription.listed",
                "License subscriptions listed"
        );
    }

    @PostMapping("/subscription/suspend")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<LicenseSubscriptionResponseDto> suspendSubscription(@RequestBody SubscriptionStatusRequest request) {
        return ApiResponse.successCode(
                licenseSubscriptionService.suspend(request.subscriptionCode(), request.reason()),
                "system.license.subscription.suspended",
                "License subscription suspended"
        );
    }

    @PostMapping("/subscription/reactivate")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<LicenseSubscriptionResponseDto> reactivateSubscription(@RequestBody SubscriptionStatusRequest request) {
        return ApiResponse.successCode(
                licenseSubscriptionService.reactivate(request.subscriptionCode()),
                "system.license.subscription.reactivated",
                "License subscription reactivated"
        );
    }

    @PostMapping("/subscription/cancel")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<LicenseSubscriptionResponseDto> cancelSubscription(@RequestBody SubscriptionStatusRequest request) {
        return ApiResponse.successCode(
                licenseSubscriptionService.cancel(request.subscriptionCode()),
                "system.license.subscription.cancelled",
                "License subscription cancelled"
        );
    }

    @PostMapping("/subscription/entitlement/save")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<Void> saveSubscriptionEntitlementOverride(@RequestBody LicenseEntitlementRequestDto request) {
        licenseSubscriptionService.saveEntitlementOverride(request);
        return ApiResponse.successCode("system.license.subscription.entitlement.saved", "License subscription entitlement saved");
    }

    @PostMapping("/key/generate")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<GeneratedLicenseKeyDto> generateLicenseKey(@RequestBody LicenseKeyRequestDto request) {
        return ApiResponse.successCode(
                licenseKeyService.generateLicenseKey(request),
                "system.license.key.generated",
                "License key generated"
        );
    }

    @PostMapping("/key/activate")
    @PreAuthorize("@privilegeAuthorizer.has(authentication, T(com.nexacore.systemmodule.privilege.bootstrap.BootstrapAdministrationPrivileges).LICENSE_ADMINISTRATION_MANAGE)")
    public ApiResponse<Boolean> activateLicenseKey(@RequestBody LicenseKeyRequestDto request) {
        return ApiResponse.successCode(
                licenseKeyService.activateLicenseKey(request),
                "system.license.key.activated",
                "License key activation checked"
        );
    }

    @PostMapping("/key/validate")
    public ApiResponse<Boolean> validateLicenseKey(@RequestBody LicenseKeyRequestDto request) {
        return ApiResponse.successCode(
                licenseKeyService.validateLicenseKey(request),
                "system.license.key.validated",
                "License key validation checked"
        );
    }

    @PostMapping("/decision/check")
    public ApiResponse<LicenseDecisionResponseDto> checkDecision(@RequestBody LicenseDecisionRequestDto request) {
        return ApiResponse.successCode(
                licenseDecisionService.check(request),
                "system.license.decision.checked",
                "License decision checked"
        );
    }

    public record SubscriptionStatusRequest(String subscriptionCode, String reason) {
    }
}
