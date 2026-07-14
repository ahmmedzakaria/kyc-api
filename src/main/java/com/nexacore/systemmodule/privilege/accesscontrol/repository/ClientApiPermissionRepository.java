package com.nexacore.systemmodule.privilege.accesscontrol.repository;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApiPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ClientApiPermissionRepository extends JpaRepository<SysClientApiPermission, Long> {
    boolean existsByClientApplicationIdAndApiRegistryIdAndActiveTrue(Long clientApplicationId, Long apiRegistryId);

    @Query("""
            select permission.apiRegistry.id
            from SysClientApiPermission permission
            where permission.clientApplication.id = :clientApplicationId
              and permission.active = true
            """)
    Set<Long> findActiveApiRegistryIdsByClientApplicationId(Long clientApplicationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysClientApiPermission permission where permission.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);
}
