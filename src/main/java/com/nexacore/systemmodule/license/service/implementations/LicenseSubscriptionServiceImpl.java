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
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LicenseSubscriptionServiceImpl implements LicenseSubscriptionService {

    private final LicenseSubscriptionRepository subscriptionRepository;
    private final LicensePlanRepository planRepository;
    private final LicenseEntitlementOverrideRepository overrideRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseSubscriptionResponseDto assignSubscription(LicenseSubscriptionRequestDto request) {
        if (request.tenantId() == null && request.businessId() == null && request.clientApplicationId() == null) {
            throw new IllegalArgumentException("At least one license owner is required");
        }

        SysLicensePlan plan = planRepository.findByPlanCode(request.planCode())
                .orElseThrow(() -> new IllegalArgumentException("License plan not found: " + request.planCode()));
        SysLicenseSubscription subscription = request.id() == null
                ? subscriptionRepository.findBySubscriptionCode(request.subscriptionCode()).orElseGet(SysLicenseSubscription::new)
                : subscriptionRepository.findById(request.id()).orElseGet(SysLicenseSubscription::new);

        subscription.setSubscriptionCode(request.subscriptionCode());
        subscription.setLicensePlan(plan);
        subscription.setTenantId(request.tenantId());
        subscription.setBusinessId(request.businessId());
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
        return subscriptionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private SysLicenseSubscription getByCode(String subscriptionCode) {
        return subscriptionRepository.findBySubscriptionCode(subscriptionCode)
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

    private SysPrivClientApplication referenceClientApplication(Long id) {
        return id == null ? null : SysPrivClientApplication.builder().id(id).build();
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

    private SysPrivApiRegistry referenceApi(Long id) {
        return id == null ? null : SysPrivApiRegistry.builder().id(id).build();
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
