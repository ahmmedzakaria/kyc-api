package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<SysPrivPrivilege, Long> {
    Optional<SysPrivPrivilege> findByPrivilegeCode(String privilegeCode);

    List<SysPrivPrivilege> findByPrivilegeCodeIn(Collection<String> privilegeCodes);

    @Query(value = """
            SELECT DISTINCT p.privilege_code
            FROM sys_priv_privileges p
            JOIN sys_priv_user_privileges up ON up.privilege_id = p.id
            WHERE up.user_id = :userId
              AND p.active = true
            """, nativeQuery = true)
    List<String> findActivePrivilegeCodesByUserId(Long userId);

    @Query(value = """
            SELECT DISTINCT p.privilege_code
            FROM sys_priv_privileges p
            JOIN sys_priv_role_privileges rp ON rp.privilege_id = p.id
            WHERE rp.role_id IN (:roleIds)
              AND p.active = true
            """, nativeQuery = true)
    List<String> findActivePrivilegeCodesByRoleIdIn(Collection<Long> roleIds);

    @Query(value="SELECT count(*) FROM sys_priv_role_privileges WHERE privilege_id=:id",nativeQuery=true)
    long countRoleDependencies(Long id);
    @Query(value="SELECT count(*) FROM sys_priv_user_privileges WHERE privilege_id=:id",nativeQuery=true)
    long countUserDependencies(Long id);
    @Query(value="SELECT count(*) FROM sys_acc_client_feature_permissions WHERE privilege_id=:id AND active=true",nativeQuery=true)
    long countClientDependencies(Long id);
    @Query(value="SELECT count(*) FROM sys_acc_api_registry WHERE required_privilege_code=:code AND active=true",nativeQuery=true)
    long countApiDependencies(String code);
    @Query(value="SELECT count(*) FROM sys_layout_route_policy_privileges WHERE privilege_id=:id AND active=true",nativeQuery=true)
    long countRouteDependencies(Long id);
    @Query(value="SELECT count(*) FROM sys_layout_ui_policy_privileges WHERE privilege_id=:id AND active=true",nativeQuery=true)
    long countUiPolicyDependencies(Long id);
    @Query(value="SELECT count(*) FROM sys_layout_feature_privileges WHERE privilege_id=:id AND active=true",nativeQuery=true)
    long countNavigationDependencies(Long id);
    @Query(value="SELECT (SELECT count(*) FROM sys_license_plan_entitlements WHERE privilege_id=:id AND active=true) + (SELECT count(*) FROM sys_license_entitlement_overrides WHERE privilege_id=:id AND active=true)",nativeQuery=true)
    long countLicenseDependencies(Long id);
}
