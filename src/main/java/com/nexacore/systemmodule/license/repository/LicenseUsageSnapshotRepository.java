package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseUsageSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseUsageSnapshotRepository extends JpaRepository<SysLicenseUsageSnapshot, Long> {
    Optional<SysLicenseUsageSnapshot> findByLicenseSubscriptionIdAndUsagePeriodAndUsageCode(
            Long licenseSubscriptionId,
            String usagePeriod,
            String usageCode
    );
}
