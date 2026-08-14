package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicensePlanEntitlement;
import com.nexacore.systemmodule.license.enums.LicenseEntitlementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LicensePlanEntitlementRepository extends JpaRepository<SysLicensePlanEntitlement, Long> {
    List<SysLicensePlanEntitlement> findByLicensePlanIdAndActiveTrue(Long licensePlanId);

    List<SysLicensePlanEntitlement> findByLicensePlanIdAndEntitlementTypeAndActiveTrue(
            Long licensePlanId,
            LicenseEntitlementType entitlementType
    );

    List<SysLicensePlanEntitlement> findByLicensePlan_PlanCodeAndActiveTrue(String planCode);
    java.util.Optional<SysLicensePlanEntitlement> findByIdAndLicensePlanPlanCode(Long id, String planCode);
}
