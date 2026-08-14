package com.nexacore.systemmodule.license.service;

import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.license.dto.*;
import com.nexacore.systemmodule.license.repository.LicenseAuditEventRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.repository.LicenseUsageSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service @RequiredArgsConstructor
public class LicenseAdministrationQueryService {
    private final DataScopeService scope;
    private final LicenseSubscriptionRepository subscriptions;
    private final LicenseUsageSnapshotRepository usage;
    private final LicenseAuditEventRepository audit;

    @Transactional(transactionManager="systemTransactionManager", readOnly=true)
    public List<LicenseUsageSnapshotDto> usage(String code) {
        Long tenant = scope.requireEffectiveTenant(null);
        requireSubscription(code, tenant);
        return usage.findByLicenseSubscriptionSubscriptionCodeAndTenantIdOrderByMeasuredAtDesc(code, tenant).stream()
                .map(x -> new LicenseUsageSnapshotDto(x.getId(), code, x.getUsagePeriod(), x.getUsageCode(), x.getUsageValue(), x.getMeasuredAt())).toList();
    }
    @Transactional(transactionManager="systemTransactionManager", readOnly=true)
    public List<LicenseAuditEventDto> audit(String code) {
        Long tenant = scope.requireEffectiveTenant(null);
        requireSubscription(code, tenant);
        return audit.findTop100ByLicenseSubscriptionSubscriptionCodeAndTenantIdOrderByCreatedAtDesc(code, tenant).stream()
                .map(x -> new LicenseAuditEventDto(x.getId(), code, x.getEventType(), x.getEventMessageCode(), x.getActorUserId(), x.getSafeContextJson(), x.getCreatedAt())).toList();
    }
    @Transactional(transactionManager="systemTransactionManager", readOnly=true)
    public LicenseRenewalSummaryDto renewal(String code) {
        Long tenant = scope.requireEffectiveTenant(null);
        var x = requireSubscription(code, tenant);
        long days = x.getExpiresAt() == null ? Long.MAX_VALUE : ChronoUnit.DAYS.between(LocalDateTime.now(), x.getExpiresAt());
        return new LicenseRenewalSummaryDto(code, x.getStatus().name(), x.getExpiresAt(), x.getGracePeriodEndsAt(), x.isAutoRenew(), days);
    }
    private com.nexacore.systemmodule.license.entity.SysLicenseSubscription requireSubscription(String code, Long tenant) {
        return subscriptions.findBySubscriptionCodeAndTenantId(code, tenant)
                .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + code));
    }
}
