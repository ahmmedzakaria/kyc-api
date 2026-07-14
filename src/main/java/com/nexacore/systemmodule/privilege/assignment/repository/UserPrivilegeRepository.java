package com.nexacore.systemmodule.privilege.assignment.repository;

import com.nexacore.systemmodule.privilege.assignment.entity.SysUserPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.UserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivilegeRepository extends JpaRepository<SysUserPrivilege, UserPrivilegeId> {
    void deleteByIdUserId(Long userId);
}
