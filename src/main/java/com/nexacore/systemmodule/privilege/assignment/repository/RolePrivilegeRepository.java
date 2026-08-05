package com.nexacore.systemmodule.privilege.assignment.repository;

import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivRolePrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePrivilegeRepository extends JpaRepository<SysPrivRolePrivilege, SysPrivRolePrivilegeId> {
    void deleteByIdRoleId(Long roleId);

    long countByIdRoleId(Long roleId);
}
