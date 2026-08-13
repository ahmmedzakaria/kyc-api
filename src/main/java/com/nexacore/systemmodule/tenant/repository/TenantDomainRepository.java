package com.nexacore.systemmodule.tenant.repository;

import com.nexacore.systemmodule.tenant.entity.SysTenantDomain;
import com.nexacore.systemmodule.tenant.entity.TenantDomainVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface TenantDomainRepository extends JpaRepository<SysTenantDomain, Long> {
    Optional<SysTenantDomain> findByHostnameAndActiveTrueAndVerificationStatus(
            String hostname, TenantDomainVerificationStatus verificationStatus);
    boolean existsByHostnameIgnoreCase(String hostname);
    List<SysTenantDomain> findByTenantId(Long tenantId);
    Optional<SysTenantDomain> findByIdAndTenantId(Long id, Long tenantId);
}
