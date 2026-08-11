package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.gatewaymodule.license.dto.LicenseUsageRecordRequestDto;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.entity.SysLicenseUsageSnapshot;
import com.nexacore.systemmodule.license.enums.LicenseStatus;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.repository.LicenseUsageSnapshotRepository;
import com.nexacore.systemmodule.license.service.interfaces.LicenseUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LicenseUsageServiceImpl implements LicenseUsageService {

    private final LicenseUsageSnapshotRepository usageRepository;
    private final LicenseSubscriptionRepository subscriptionRepository;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public void recordUsage(LicenseUsageRecordRequestDto request) {
        SysLicenseSubscription subscription = resolveSubscription(request);
        String period = normalizePeriod(request.usagePeriod());
        String usageCode = normalizeCode(request.usageCode());
        long increment = request.incrementBy() == null ? 1L : request.incrementBy();

        SysLicenseUsageSnapshot snapshot = usageRepository
                .findByLicenseSubscriptionIdAndUsagePeriodAndUsageCode(subscription.getId(), period, usageCode)
                .orElseGet(() -> SysLicenseUsageSnapshot.builder()
                        .licenseSubscription(subscription)
                        .tenantId(subscription.getTenantId())
                        .businessId(subscription.getBusinessId())
                        .usagePeriod(period)
                        .usageCode(usageCode)
                        .usageValue(0L)
                        .build());
        snapshot.setUsageValue(snapshot.getUsageValue() + increment);
        snapshot.setMeasuredAt(LocalDateTime.now());
        usageRepository.save(snapshot);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public long getUsageValue(String subscriptionCode, String usagePeriod, String usageCode) {
        SysLicenseSubscription subscription = subscriptionRepository.findBySubscriptionCodeAndTenantId(
                        subscriptionCode, dataScopeService.requireEffectiveTenant(null))
                .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + subscriptionCode));
        return usageRepository.findByLicenseSubscriptionIdAndUsagePeriodAndUsageCode(
                        subscription.getId(),
                        normalizePeriod(usagePeriod),
                        normalizeCode(usageCode)
                )
                .map(SysLicenseUsageSnapshot::getUsageValue)
                .orElse(0L);
    }

    private SysLicenseSubscription resolveSubscription(LicenseUsageRecordRequestDto request) {
        long tenantId = dataScopeService.requireEffectiveTenant(request.tenantId());
        if (request.subscriptionCode() != null && !request.subscriptionCode().isBlank()) {
            return subscriptionRepository.findBySubscriptionCodeAndTenantId(request.subscriptionCode(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + request.subscriptionCode()));
        }
        return subscriptionRepository.findDecisionCandidates(
                        tenantId,
                        request.businessId(),
                        request.clientApplicationId(),
                        List.of(LicenseStatus.ACTIVE, LicenseStatus.TRIAL, LicenseStatus.GRACE_PERIOD)
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No active license subscription found"));
    }

    private String normalizePeriod(String value) {
        return value == null || value.isBlank()
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
                : value.trim();
    }

    private String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Usage code is required");
        }
        return value.trim().toUpperCase();
    }
}
