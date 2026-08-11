package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseEntitlementOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicenseEntitlementOverrideRepository extends JpaRepository<SysLicenseEntitlementOverride, Long> {
    List<SysLicenseEntitlementOverride> findByLicenseSubscriptionIdAndActiveTrue(Long licenseSubscriptionId);

    List<SysLicenseEntitlementOverride> findByLicenseSubscriptionSubscriptionCodeAndLicenseSubscriptionTenantIdAndActiveTrue(
            String subscriptionCode,
            Long tenantId
    );
}
