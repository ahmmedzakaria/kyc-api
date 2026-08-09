package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientFeaturePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ClientFeaturePermissionRepository extends JpaRepository<SysAccClientFeaturePermission, Long> {
    boolean existsByClientApplicationIdAndPrivilegePrivilegeCodeAndActiveTrue(Long clientApplicationId, String privilegeCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysAccClientFeaturePermission permission where permission.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);

    @Query("""
            select p.privilegeCode
            from SysAccClientFeaturePermission fp
            join fp.privilege p
            where fp.clientApplication.id = :clientApplicationId
              and fp.active = true
              and p.active = true
            """)
    Set<String> findActivePrivilegeCodesByClientApplicationId(Long clientApplicationId);

    @Query("""
            select fp
            from SysAccClientFeaturePermission fp
            join fetch fp.privilege p
            where fp.clientApplication.id = :clientApplicationId
              and fp.active = true
              and p.privilegeCode in :privilegeCodes
            """)
    List<SysAccClientFeaturePermission> findActiveByClientApplicationIdAndPrivilegeCodes(Long clientApplicationId,
                                                                                      Collection<String> privilegeCodes);
}
