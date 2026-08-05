package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;
import com.nexacore.systemmodule.license.entity.SysLicenseEntitlementOverride;
import com.nexacore.systemmodule.license.entity.SysLicensePlanEntitlement;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.entity.SysLicenseUsageSnapshot;
import com.nexacore.systemmodule.license.enums.LicenseDecisionCode;
import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.license.enums.LicenseOverrideMode;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.license.repository.LicenseEntitlementOverrideRepository;
import com.nexacore.systemmodule.license.repository.LicensePlanEntitlementRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.repository.LicenseUsageSnapshotRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import com.nexacore.systemmodule.license.service.interfaces.LicenseDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicenseDecisionServiceImpl implements LicenseDecisionService {

    private final LicenseSubscriptionRepository subscriptionRepository;
    private final LicensePlanEntitlementRepository planEntitlementRepository;
    private final LicenseEntitlementOverrideRepository overrideRepository;
    private final LicenseUsageSnapshotRepository usageRepository;
    private final ModuleRepository moduleRepository;
    private final FeatureRepository featureRepository;
    private final PrivilegeRepository privilegeRepository;
    private final ApiRegistryRepository apiRegistryRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public LicenseDecisionResponseDto check(LicenseDecisionRequestDto request) {
        LicenseEntitlementType entitlementType = resolveEntitlementType(request);
        if (request.tenantId() == null && request.businessId() == null && request.clientApplicationId() == null) {
            return denied(LicenseDecisionCode.MISSING_CONTEXT, "system.license.context.missing", "License context is required", null);
        }

        Optional<SysLicenseSubscription> subscription = subscriptionRepository.findDecisionCandidates(
                        request.tenantId(),
                        request.businessId(),
                        request.clientApplicationId(),
                        List.of(LicenseStatus.ACTIVE, LicenseStatus.TRIAL, LicenseStatus.GRACE_PERIOD)
                )
                .stream()
                .filter(this::isEffective)
                .findFirst();

        if (subscription.isEmpty()) {
            return denied(LicenseDecisionCode.NO_ACTIVE_SUBSCRIPTION, "system.license.subscription.missing", "No active license subscription found", null);
        }

        SysLicenseSubscription activeSubscription = subscription.get();
        LicenseDecisionResponseDto statusDecision = statusDecision(activeSubscription);
        if (statusDecision != null) {
            return statusDecision;
        }

        ResolvedTarget target = resolveTarget(request, entitlementType);
        List<SysLicenseEntitlementOverride> overrides = overrideRepository.findByLicenseSubscriptionIdAndActiveTrue(activeSubscription.getId());
        if (overrides.stream().anyMatch(override -> override.getOverrideMode() == LicenseOverrideMode.DENY
                && matchesOverride(override, entitlementType, target))) {
            return denied(LicenseDecisionCode.ENTITLEMENT_DENIED, "system.license.entitlement.denied", "License entitlement is denied", activeSubscription);
        }

        Optional<SysLicenseEntitlementOverride> allowOverride = overrides.stream()
                .filter(override -> override.getOverrideMode() == LicenseOverrideMode.ALLOW
                        && matchesOverride(override, entitlementType, target))
                .findFirst();

        if (entitlementType == LicenseEntitlementType.LIMIT) {
            return checkLimit(request, activeSubscription, overrides);
        }

        boolean planAllows = planEntitlementRepository
                .findByLicensePlanIdAndEntitlementTypeAndActiveTrue(activeSubscription.getLicensePlan().getId(), entitlementType)
                .stream()
                .anyMatch(entitlement -> matchesPlanEntitlement(entitlement, entitlementType, target));
        if (planAllows || allowOverride.isPresent()) {
            return allowed(activeSubscription, null);
        }
        return denied(LicenseDecisionCode.ENTITLEMENT_DENIED, "system.license.entitlement.denied", "License entitlement is not included", activeSubscription);
    }

    private LicenseDecisionResponseDto checkLimit(LicenseDecisionRequestDto request,
                                                 SysLicenseSubscription subscription,
                                                 List<SysLicenseEntitlementOverride> overrides) {
        String limitCode = normalizeCode(request.limitCode());
        Optional<SysLicenseEntitlementOverride> limitOverride = overrides.stream()
                .filter(override -> override.getOverrideMode() == LicenseOverrideMode.LIMIT_OVERRIDE
                        && normalizeCode(override.getLimitCode()).equals(limitCode))
                .findFirst();
        Optional<SysLicensePlanEntitlement> planLimit = planEntitlementRepository
                .findByLicensePlanIdAndEntitlementTypeAndActiveTrue(subscription.getLicensePlan().getId(), LicenseEntitlementType.LIMIT)
                .stream()
                .filter(entitlement -> normalizeCode(entitlement.getLimitCode()).equals(limitCode))
                .findFirst();

        Long limitValue = limitOverride.map(SysLicenseEntitlementOverride::getLimitValue)
                .or(() -> planLimit.map(SysLicensePlanEntitlement::getLimitValue))
                .orElse(null);
        if (limitValue == null) {
            return denied(LicenseDecisionCode.ENTITLEMENT_DENIED, "system.license.entitlement.denied", "License limit is not configured", subscription);
        }

        long currentUsage = usageRepository.findByLicenseSubscriptionIdAndUsagePeriodAndUsageCode(
                        subscription.getId(),
                        currentUsagePeriod(),
                        limitCode
                )
                .map(SysLicenseUsageSnapshot::getUsageValue)
                .orElse(0L);
        long requestedUsage = request.requestedUsage() == null ? 1L : request.requestedUsage();
        long remaining = limitValue - currentUsage - requestedUsage;
        if (remaining < 0) {
            return denied(LicenseDecisionCode.LIMIT_EXCEEDED, "system.license.limit.exceeded", "License usage limit is exceeded", subscription);
        }
        return allowed(subscription, remaining);
    }

    private LicenseDecisionResponseDto statusDecision(SysLicenseSubscription subscription) {
        return switch (subscription.getStatus()) {
            case SUSPENDED -> denied(LicenseDecisionCode.LICENSE_SUSPENDED, "system.license.subscription.suspended", "License is suspended", subscription);
            case CANCELLED -> denied(LicenseDecisionCode.LICENSE_CANCELLED, "system.license.subscription.cancelled", "License is cancelled", subscription);
            case REVOKED -> denied(LicenseDecisionCode.LICENSE_REVOKED, "system.license.subscription.revoked", "License is revoked", subscription);
            case EXPIRED -> denied(LicenseDecisionCode.LICENSE_EXPIRED, "system.license.subscription.expired", "License is expired", subscription);
            default -> null;
        };
    }

    private boolean isEffective(SysLicenseSubscription subscription) {
        LocalDateTime now = LocalDateTime.now();
        if (subscription.getStartsAt() != null && subscription.getStartsAt().isAfter(now)) {
            return false;
        }
        if (subscription.getExpiresAt() == null || !subscription.getExpiresAt().isBefore(now)) {
            return true;
        }
        return subscription.getStatus() == LicenseStatus.GRACE_PERIOD
                && subscription.getGracePeriodEndsAt() != null
                && !subscription.getGracePeriodEndsAt().isBefore(now);
    }

    private ResolvedTarget resolveTarget(LicenseDecisionRequestDto request, LicenseEntitlementType entitlementType) {
        return new ResolvedTarget(
                request.moduleId() != null ? request.moduleId() : resolveModuleId(request),
                request.submoduleId(),
                request.featureId() != null ? request.featureId() : resolveFeatureId(request),
                request.privilegeId() != null ? request.privilegeId() : resolvePrivilegeId(request),
                request.apiRegistryId() != null ? request.apiRegistryId() : resolveApiId(request),
                entitlementType == LicenseEntitlementType.LIMIT ? normalizeCode(request.limitCode()) : null
        );
    }

    private Long resolveModuleId(LicenseDecisionRequestDto request) {
        return request.moduleCode() == null ? null : moduleRepository.findByCode(request.moduleCode()).map(module -> module.getId()).orElse(null);
    }

    private Long resolveFeatureId(LicenseDecisionRequestDto request) {
        if (request.moduleCode() == null || request.submoduleCode() == null
                || request.featureTypeCode() == null || request.featureCode() == null) {
            return null;
        }
        return featureRepository.findBySubmoduleModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndCode(
                        request.moduleCode(),
                        request.submoduleCode(),
                        request.featureTypeCode(),
                        request.featureCode()
                )
                .map(feature -> feature.getId())
                .orElse(null);
    }

    private Long resolvePrivilegeId(LicenseDecisionRequestDto request) {
        return request.privilegeCode() == null
                ? null
                : privilegeRepository.findByPrivilegeCode(request.privilegeCode()).map(privilege -> privilege.getId()).orElse(null);
    }

    private Long resolveApiId(LicenseDecisionRequestDto request) {
        return request.apiCode() == null
                ? null
                : apiRegistryRepository.findByApiCode(request.apiCode()).map(api -> api.getId()).orElse(null);
    }

    private boolean matchesPlanEntitlement(SysLicensePlanEntitlement entitlement,
                                           LicenseEntitlementType entitlementType,
                                           ResolvedTarget target) {
        return switch (entitlementType) {
            case MODULE -> entitlement.getModule() != null && entitlement.getModule().getId().equals(target.moduleId());
            case SUBMODULE -> entitlement.getSubmodule() != null && entitlement.getSubmodule().getId().equals(target.submoduleId());
            case FEATURE -> entitlement.getFeature() != null && entitlement.getFeature().getId().equals(target.featureId());
            case ACTION -> entitlement.getPrivilege() != null && entitlement.getPrivilege().getId().equals(target.privilegeId());
            case API -> entitlement.getApiRegistry() != null && entitlement.getApiRegistry().getId().equals(target.apiRegistryId());
            case ADD_ON -> false;
            case LIMIT -> entitlement.getLimitCode() != null && normalizeCode(entitlement.getLimitCode()).equals(target.limitCode());
        };
    }

    private boolean matchesOverride(SysLicenseEntitlementOverride override,
                                    LicenseEntitlementType entitlementType,
                                    ResolvedTarget target) {
        return switch (entitlementType) {
            case MODULE -> override.getModule() != null && override.getModule().getId().equals(target.moduleId());
            case SUBMODULE -> override.getSubmodule() != null && override.getSubmodule().getId().equals(target.submoduleId());
            case FEATURE -> override.getFeature() != null && override.getFeature().getId().equals(target.featureId());
            case ACTION -> override.getPrivilege() != null && override.getPrivilege().getId().equals(target.privilegeId());
            case API -> override.getApiRegistry() != null && override.getApiRegistry().getId().equals(target.apiRegistryId());
            case ADD_ON -> false;
            case LIMIT -> override.getLimitCode() != null && normalizeCode(override.getLimitCode()).equals(target.limitCode());
        };
    }

    private LicenseEntitlementType resolveEntitlementType(LicenseDecisionRequestDto request) {
        if (request.entitlementType() != null && !request.entitlementType().isBlank()) {
            return LicenseEntitlementType.valueOf(request.entitlementType().trim().toUpperCase(Locale.ROOT));
        }
        if (request.apiRegistryId() != null || request.apiCode() != null) {
            return LicenseEntitlementType.API;
        }
        if (request.limitCode() != null) {
            return LicenseEntitlementType.LIMIT;
        }
        if (request.privilegeId() != null || request.privilegeCode() != null) {
            return LicenseEntitlementType.ACTION;
        }
        if (request.featureId() != null || request.featureCode() != null) {
            return LicenseEntitlementType.FEATURE;
        }
        return LicenseEntitlementType.MODULE;
    }

    private LicenseDecisionResponseDto allowed(SysLicenseSubscription subscription, Long remainingUsage) {
        return LicenseDecisionResponseDto.builder()
                .allowed(true)
                .decisionCode(LicenseDecisionCode.ALLOWED.name())
                .messageCode("system.license.allowed")
                .fallbackMessage("License allows this action")
                .licenseStatus(subscription.getStatus().name())
                .subscriptionCode(subscription.getSubscriptionCode())
                .planCode(subscription.getLicensePlan().getPlanCode())
                .expiresAt(subscription.getExpiresAt())
                .remainingUsage(remainingUsage)
                .build();
    }

    private LicenseDecisionResponseDto denied(LicenseDecisionCode decisionCode,
                                             String messageCode,
                                             String fallbackMessage,
                                             SysLicenseSubscription subscription) {
        return LicenseDecisionResponseDto.builder()
                .allowed(false)
                .decisionCode(decisionCode.name())
                .messageCode(messageCode)
                .fallbackMessage(fallbackMessage)
                .licenseStatus(subscription == null ? null : subscription.getStatus().name())
                .subscriptionCode(subscription == null ? null : subscription.getSubscriptionCode())
                .planCode(subscription == null ? null : subscription.getLicensePlan().getPlanCode())
                .deniedReason(decisionCode.name())
                .expiresAt(subscription == null ? null : subscription.getExpiresAt())
                .build();
    }

    private String currentUsagePeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private record ResolvedTarget(
            Long moduleId,
            Long submoduleId,
            Long featureId,
            Long privilegeId,
            Long apiRegistryId,
            String limitCode
    ) {
    }
}
