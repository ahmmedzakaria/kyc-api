package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.systemmodule.license.dto.LicenseEntitlementRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseEntitlementResponseDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseSubscriptionResponseDto;
import com.nexacore.systemmodule.license.entity.SysLicenseEntitlementOverride;
import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.enums.LicenseOverrideMode;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.license.repository.LicenseEntitlementOverrideRepository;
import com.nexacore.systemmodule.license.repository.LicensePlanRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.service.interfaces.LicenseSubscriptionService;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LicenseSubscriptionServiceImpl implements LicenseSubscriptionService {

    private final LicenseSubscriptionRepository subscriptionRepository;
    private final LicensePlanRepository planRepository;
    private final LicenseEntitlementOverrideRepository overrideRepository;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseSubscriptionResponseDto assignSubscription(LicenseSubscriptionRequestDto request) {
        UserScopeAssignment scope = dataScopeService.requireWritableScope(request.tenantId(), request.businessId(), null);

        SysLicensePlan plan = planRepository.findByPlanCode(request.planCode())
                .orElseThrow(() -> new IllegalArgumentException("License plan not found: " + request.planCode()));
        SysLicenseSubscription subscription = request.id() == null
                ? subscriptionRepository.findBySubscriptionCodeAndTenantId(request.subscriptionCode(), scope.tenantId())
                    .orElseGet(SysLicenseSubscription::new)
                : subscriptionRepository.findByIdAndTenantId(request.id(), scope.tenantId())
                    .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + request.id()));

        subscription.setSubscriptionCode(request.subscriptionCode());
        subscription.setLicensePlan(plan);
        subscription.setTenantId(scope.tenantId());
        subscription.setBusinessId(scope.businessId());
        subscription.setClientApplication(referenceClientApplication(request.clientApplicationId()));
        subscription.setStatus(request.status() == null ? LicenseStatus.ACTIVE : request.status());
        subscription.setStartsAt(request.startsAt());
        subscription.setExpiresAt(request.expiresAt());
        subscription.setGracePeriodEndsAt(request.gracePeriodEndsAt());
        subscription.setAutoRenew(Boolean.TRUE.equals(request.autoRenew()));
        subscription.setMetadataJson(request.metadataJson());

        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseSubscriptionResponseDto suspend(String subscriptionCode, String reason) {
        SysLicenseSubscription subscription = getByCode(subscriptionCode);
        subscription.setStatus(LicenseStatus.SUSPENDED);
        subscription.setSuspendedAt(LocalDateTime.now());
        subscription.setSuspensionReason(reason);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseSubscriptionResponseDto reactivate(String subscriptionCode) {
        SysLicenseSubscription subscription = getByCode(subscriptionCode);
        subscription.setStatus(LicenseStatus.ACTIVE);
        subscription.setSuspendedAt(null);
        subscription.setSuspensionReason(null);
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseSubscriptionResponseDto cancel(String subscriptionCode) {
        SysLicenseSubscription subscription = getByCode(subscriptionCode);
        subscription.setStatus(LicenseStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        return toResponse(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void saveEntitlementOverride(LicenseEntitlementRequestDto request) {
        SysLicenseSubscription subscription = getByCode(request.subscriptionCode());
        overrideRepository.save(SysLicenseEntitlementOverride.builder()
                .licenseSubscription(subscription)
                .entitlementType(request.entitlementType())
                .module(referenceModule(request.moduleId()))
                .submodule(referenceSubmodule(request.submoduleId()))
                .feature(referenceFeature(request.featureId()))
                .privilege(referencePrivilege(request.privilegeId()))
                .apiRegistry(referenceApi(request.apiRegistryId()))
                .limitCode(normalizeCode(request.limitCode()))
                .limitValue(request.limitValue())
                .overrideMode(request.overrideMode() == null ? LicenseOverrideMode.ALLOW : request.overrideMode())
                .active(request.active() == null || request.active())
                .build());
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LicenseEntitlementResponseDto> listSubscriptionEntitlements(String subscriptionCode) {
        return overrideRepository.findByLicenseSubscription_SubscriptionCodeAndActiveTrue(subscriptionCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private LicenseEntitlementResponseDto toResponse(SysLicenseEntitlementOverride override) {
        return LicenseEntitlementResponseDto.builder()
                .id(override.getId())
                .ownerCode(override.getLicenseSubscription().getSubscriptionCode())
                .entitlementType(override.getEntitlementType())
                .moduleId(override.getModule() == null ? null : override.getModule().getId())
                .submoduleId(override.getSubmodule() == null ? null : override.getSubmodule().getId())
                .featureId(override.getFeature() == null ? null : override.getFeature().getId())
                .privilegeId(override.getPrivilege() == null ? null : override.getPrivilege().getId())
                .apiRegistryId(override.getApiRegistry() == null ? null : override.getApiRegistry().getId())
                .limitCode(override.getLimitCode())
                .limitValue(override.getLimitValue())
                .overrideMode(override.getOverrideMode())
                .active(override.isActive())
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LicenseSubscriptionResponseDto> listSubscriptions() {
        return subscriptionRepository.findByTenantIdOrderByIdDesc(dataScopeService.requireEffectiveTenant(null)).stream()
                .map(this::toResponse)
                .toList();
    }

    private SysLicenseSubscription getByCode(String subscriptionCode) {
        return subscriptionRepository.findBySubscriptionCodeAndTenantId(
                        subscriptionCode, dataScopeService.requireEffectiveTenant(null))
                .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + subscriptionCode));
    }

    private LicenseSubscriptionResponseDto toResponse(SysLicenseSubscription subscription) {
        return LicenseSubscriptionResponseDto.builder()
                .id(subscription.getId())
                .subscriptionCode(subscription.getSubscriptionCode())
                .planCode(subscription.getLicensePlan().getPlanCode())
                .tenantId(subscription.getTenantId())
                .businessId(subscription.getBusinessId())
                .clientApplicationId(subscription.getClientApplication() == null ? null : subscription.getClientApplication().getId())
                .status(subscription.getStatus())
                .startsAt(subscription.getStartsAt())
                .expiresAt(subscription.getExpiresAt())
                .gracePeriodEndsAt(subscription.getGracePeriodEndsAt())
                .autoRenew(subscription.isAutoRenew())
                .build();
    }

    private SysAccClientApplication referenceClientApplication(Long id) {
        return id == null ? null : SysAccClientApplication.builder().id(id).build();
    }

    private SysPrivModule referenceModule(Long id) {
        return id == null ? null : SysPrivModule.builder().id(id).build();
    }

    private SysPrivSubmodule referenceSubmodule(Long id) {
        return id == null ? null : SysPrivSubmodule.builder().id(id).build();
    }

    private SysPrivFeature referenceFeature(Long id) {
        return id == null ? null : SysPrivFeature.builder().id(id).build();
    }

    private SysPrivPrivilege referencePrivilege(Long id) {
        return id == null ? null : SysPrivPrivilege.builder().id(id).build();
    }

    private SysAccApiRegistry referenceApi(Long id) {
        return id == null ? null : SysAccApiRegistry.builder().id(id).build();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
