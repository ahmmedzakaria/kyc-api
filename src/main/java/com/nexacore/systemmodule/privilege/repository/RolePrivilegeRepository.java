package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.RolePrivilege;
import com.nexacore.systemmodule.privilege.entity.RolePrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePrivilegeRepository extends JpaRepository<RolePrivilege, RolePrivilegeId> {
    void deleteByIdRoleId(Long roleId);

    long countByIdRoleId(Long roleId);
}
