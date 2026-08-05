package com.nexacore.systemmodule.privilege.assignment.repository;

import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilege;
import com.nexacore.systemmodule.privilege.assignment.entity.SysPrivUserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivilegeRepository extends JpaRepository<SysPrivUserPrivilege, SysPrivUserPrivilegeId> {
    void deleteByIdUserId(Long userId);
}
