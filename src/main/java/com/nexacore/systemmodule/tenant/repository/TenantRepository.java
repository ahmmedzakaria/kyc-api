package com.nexacore.systemmodule.tenant.repository;

import com.nexacore.systemmodule.tenant.entity.SysTenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;

public interface TenantRepository extends JpaRepository<SysTenant, Long> {
    Optional<SysTenant> findByTenantCodeIgnoreCase(String tenantCode);
    List<SysTenant> findAllByOrderByTenantCodeAsc();
    List<SysTenant> findByStatusOrderByTenantCodeAsc(TenantStatus status);
    List<SysTenant> findByIdInAndStatusOrderByTenantCodeAsc(java.util.Collection<Long> ids, TenantStatus status);
    long countByStatus(TenantStatus status);
}
