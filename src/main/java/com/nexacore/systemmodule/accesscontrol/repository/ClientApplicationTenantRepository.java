package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplicationTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClientApplicationTenantRepository extends JpaRepository<SysAccClientApplicationTenant, Long> {
    List<SysAccClientApplicationTenant> findByClientApplicationIdAndActiveTrue(Long clientApplicationId);
    boolean existsByClientApplicationIdAndBusinessIdAndActiveTrue(Long clientApplicationId, Long businessId);

    boolean existsByClientApplicationIdAndTenantIdAndActiveTrue(Long clientApplicationId, Long tenantId);
    long countByTenantIdAndActiveTrue(Long tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysAccClientApplicationTenant tenant where tenant.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);
}
