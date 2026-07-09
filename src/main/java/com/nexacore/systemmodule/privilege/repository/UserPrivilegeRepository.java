package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.SysUserPrivilege;
import com.nexacore.systemmodule.privilege.entity.UserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivilegeRepository extends JpaRepository<SysUserPrivilege, UserPrivilegeId> {
    void deleteByIdUserId(Long userId);
}
