package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LicenseKeyRepository extends JpaRepository<SysLicenseKey, Long> {
    Optional<SysLicenseKey> findByLicenseKeyHashAndLicenseSubscriptionTenantId(String licenseKeyHash, Long tenantId);

    List<SysLicenseKey> findByLicenseSubscriptionSubscriptionCodeAndLicenseSubscriptionTenantIdOrderByIssuedAtDesc(
            String subscriptionCode,
            Long tenantId
    );
    Optional<SysLicenseKey> findByIdAndLicenseSubscriptionTenantId(Long id, Long tenantId);
}
