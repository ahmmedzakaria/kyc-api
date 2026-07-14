package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseKeyRepository extends JpaRepository<SysLicenseKey, Long> {
    Optional<SysLicenseKey> findByLicenseKeyHash(String licenseKeyHash);
}
