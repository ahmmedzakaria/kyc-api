package com.nexacore.systemmodule.privilege.accesscontrol.repository;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplicationTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ClientApplicationTenantRepository extends JpaRepository<SysClientApplicationTenant, Long> {
    boolean existsByClientApplicationIdAndBusinessIdAndActiveTrue(Long clientApplicationId, Long businessId);

    boolean existsByClientApplicationIdAndTenantIdAndActiveTrue(Long clientApplicationId, Long tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysClientApplicationTenant tenant where tenant.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);
}
