package com.nexacore.systemmodule.tenant.repository;

import com.nexacore.systemmodule.tenant.entity.SysTenantDomain;
import com.nexacore.systemmodule.tenant.entity.TenantDomainVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantDomainRepository extends JpaRepository<SysTenantDomain, Long> {
    List<SysTenantDomain> findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
            String hostname, TenantDomainVerificationStatus verificationStatus);
    boolean existsByHostnameIgnoreCase(String hostname);
    List<SysTenantDomain> findByTenantId(Long tenantId);
    Optional<SysTenantDomain> findByIdAndTenantId(Long id, Long tenantId);
}
