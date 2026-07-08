package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.Privilege;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    Optional<Privilege> findByPrivilegeCode(String privilegeCode);

    List<Privilege> findByPrivilegeCodeIn(Collection<String> privilegeCodes);

    @Query(value = """
            SELECT DISTINCT p.privilege_code
            FROM sys_privileges p
            JOIN sys_user_privileges up ON up.privilege_id = p.id
            WHERE up.user_id = :userId
              AND p.active = true
            """, nativeQuery = true)
    List<String> findActivePrivilegeCodesByUserId(Long userId);

    @Query(value = """
            SELECT DISTINCT p.privilege_code
            FROM sys_privileges p
            JOIN sys_role_privileges rp ON rp.privilege_id = p.id
            WHERE rp.role_id IN (:roleIds)
              AND p.active = true
            """, nativeQuery = true)
    List<String> findActivePrivilegeCodesByRoleIdIn(Collection<Long> roleIds);
}
