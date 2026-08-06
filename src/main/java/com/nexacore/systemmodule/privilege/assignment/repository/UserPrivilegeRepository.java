package com.nexacore.systemmodule.privilege.assignment.repository;

import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserPrivilegeRepository extends JpaRepository<SysPrivUserPrivilege, SysPrivUserPrivilegeId> {
    void deleteByIdUserId(Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional(transactionManager = "systemTransactionManager")
    @Query(value = """
            DELETE FROM sys_priv_user_privileges assignment
            USING sys_priv_privileges privilege
            WHERE assignment.privilege_id = privilege.id
              AND assignment.user_id = :userId
              AND privilege.module_code = :moduleCode
            """, nativeQuery = true)
    void deleteByUserIdAndPrivilegeModuleCode(@Param("userId") Long userId,
                                               @Param("moduleCode") String moduleCode);
}
