package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.gatewaymodule.license.dto.LicenseDecisionRequestDto;
import com.nexacore.gatewaymodule.license.dto.LicenseDecisionResponseDto;
import com.nexacore.systemmodule.license.entity.SysLicensePlan;
import com.nexacore.systemmodule.license.entity.SysLicensePlanEntitlement;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.entity.SysLicenseUsageSnapshot;
import com.nexacore.systemmodule.license.enums.LicenseDecisionCode;
import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.license.repository.LicenseEntitlementOverrideRepository;
import com.nexacore.systemmodule.license.repository.LicensePlanEntitlementRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.repository.LicenseUsageSnapshotRepository;
import com.nexacore.systemmodule.privilege.accesscontrol.repository.ApiRegistryRepository;
import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import com.nexacore.systemmodule.privilege.catalog.repository.FeatureRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.ModuleRepository;
import com.nexacore.systemmodule.privilege.catalog.repository.PrivilegeRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LicenseDecisionServiceImplTest {

    private final LicenseSubscriptionRepository subscriptionRepository = mock(LicenseSubscriptionRepository.class);
    private final LicensePlanEntitlementRepository entitlementRepository = mock(LicensePlanEntitlementRepository.class);
    private final LicenseEntitlementOverrideRepository overrideRepository = mock(LicenseEntitlementOverrideRepository.class);
    private final LicenseUsageSnapshotRepository usageRepository = mock(LicenseUsageSnapshotRepository.class);
    private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
    private final FeatureRepository featureRepository = mock(FeatureRepository.class);
    private final PrivilegeRepository privilegeRepository = mock(PrivilegeRepository.class);
    private final ApiRegistryRepository apiRegistryRepository = mock(ApiRegistryRepository.class);
    private final LicenseDecisionServiceImpl service = new LicenseDecisionServiceImpl(
            subscriptionRepository,
            entitlementRepository,
            overrideRepository,
            usageRepository,
            moduleRepository,
            featureRepository,
            privilegeRepository,
            apiRegistryRepository
    );

    @Test
    void allowsLicensedModuleEntitlement() {
        SysLicenseSubscription subscription = activeSubscription();
        when(subscriptionRepository.findDecisionCandidates(any(), any(), any(), any()))
                .thenReturn(List.of(subscription));
        when(overrideRepository.findByLicenseSubscriptionIdAndActiveTrue(20L)).thenReturn(List.of());
        when(entitlementRepository.findByLicensePlanIdAndEntitlementTypeAndActiveTrue(10L, LicenseEntitlementType.MODULE))
                .thenReturn(List.of(SysLicensePlanEntitlement.builder()
                        .module(SysPrivModule.builder().id(30L).build())
                        .active(true)
                        .build()));

        LicenseDecisionResponseDto decision = service.check(LicenseDecisionRequestDto.builder()
                .tenantId(1L)
                .entitlementType("MODULE")
                .moduleId(30L)
                .build());

        assertThat(decision.allowed()).isTrue();
        assertThat(decision.decisionCode()).isEqualTo(LicenseDecisionCode.ALLOWED.name());
        assertThat(decision.subscriptionCode()).isEqualTo("SUB-001");
    }

    @Test
    void deniesWhenLimitWouldBeExceeded() {
        SysLicenseSubscription subscription = activeSubscription();
        String period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        when(subscriptionRepository.findDecisionCandidates(any(), any(), any(), any()))
                .thenReturn(List.of(subscription));
        when(overrideRepository.findByLicenseSubscriptionIdAndActiveTrue(20L)).thenReturn(List.of());
        when(entitlementRepository.findByLicensePlanIdAndEntitlementTypeAndActiveTrue(10L, LicenseEntitlementType.LIMIT))
                .thenReturn(List.of(SysLicensePlanEntitlement.builder()
                        .limitCode("MONTHLY_INVOICES")
                        .limitValue(10L)
                        .active(true)
                        .build()));
        when(usageRepository.findByLicenseSubscriptionIdAndUsagePeriodAndUsageCode(20L, period, "MONTHLY_INVOICES"))
                .thenReturn(Optional.of(SysLicenseUsageSnapshot.builder().usageValue(10L).build()));

        LicenseDecisionResponseDto decision = service.check(LicenseDecisionRequestDto.builder()
                .tenantId(1L)
                .entitlementType("LIMIT")
                .limitCode("monthly_invoices")
                .requestedUsage(1L)
                .build());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.decisionCode()).isEqualTo(LicenseDecisionCode.LIMIT_EXCEEDED.name());
    }

    private SysLicenseSubscription activeSubscription() {
        return SysLicenseSubscription.builder()
                .id(20L)
                .subscriptionCode("SUB-001")
                .licensePlan(SysLicensePlan.builder().id(10L).planCode("PLAN-001").build())
                .tenantId(1L)
                .status(LicenseStatus.ACTIVE)
                .startsAt(LocalDateTime.now().minusDays(1))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }
}
