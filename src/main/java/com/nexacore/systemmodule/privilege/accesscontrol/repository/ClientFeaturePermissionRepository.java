package com.nexacore.systemmodule.privilege.accesscontrol.repository;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientFeaturePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ClientFeaturePermissionRepository extends JpaRepository<SysPrivClientFeaturePermission, Long> {
    boolean existsByClientApplicationIdAndPrivilegePrivilegeCodeAndActiveTrue(Long clientApplicationId, String privilegeCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysPrivClientFeaturePermission permission where permission.clientApplication.id = :clientApplicationId")
    void deleteByClientApplicationId(Long clientApplicationId);

    @Query("""
            select p.privilegeCode
            from SysPrivClientFeaturePermission fp
            join fp.privilege p
            where fp.clientApplication.id = :clientApplicationId
              and fp.active = true
              and p.active = true
            """)
    Set<String> findActivePrivilegeCodesByClientApplicationId(Long clientApplicationId);

    @Query("""
            select fp
            from SysPrivClientFeaturePermission fp
            join fetch fp.privilege p
            where fp.clientApplication.id = :clientApplicationId
              and fp.active = true
              and p.privilegeCode in :privilegeCodes
            """)
    List<SysPrivClientFeaturePermission> findActiveByClientApplicationIdAndPrivilegeCodes(Long clientApplicationId,
                                                                                      Collection<String> privilegeCodes);
}
