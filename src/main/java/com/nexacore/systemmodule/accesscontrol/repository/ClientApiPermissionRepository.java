package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApiPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface ClientApiPermissionRepository extends JpaRepository<SysPrivClientApiPermission, Long> {
    boolean existsByClientApplicationIdAndApiRegistryIdAndActiveTrue(Long clientApplicationId, Long apiRegistryId);

    @Query("""
            select permission.apiRegistry.id
            from SysPrivClientApiPermission permission
            where permission.clientApplication.id = :clientApplicationId
              and permission.active = true
            """)
    Set<Long> findActiveApiRegistryIdsByClientApplicationId(Long clientApplicationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysPrivClientApiPermission permission where permission.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);
}
