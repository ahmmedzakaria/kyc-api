package com.nexacore.systemmodule.tenant.repository;

import com.nexacore.systemmodule.tenant.entity.SysTenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<SysTenant, Long> {
    Optional<SysTenant> findByTenantCodeIgnoreCase(String tenantCode);
    List<SysTenant> findAllByOrderByTenantCodeAsc();
}
