package com.nexacore.systemmodule.privilege.assignment.repository;

import com.nexacore.systemmodule.privilege.assignment.entity.SysRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.RolePrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePrivilegeRepository extends JpaRepository<SysRolePrivilege, RolePrivilegeId> {
    void deleteByIdRoleId(Long roleId);

    long countByIdRoleId(Long roleId);
}
