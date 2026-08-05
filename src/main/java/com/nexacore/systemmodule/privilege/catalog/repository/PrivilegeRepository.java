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
}
