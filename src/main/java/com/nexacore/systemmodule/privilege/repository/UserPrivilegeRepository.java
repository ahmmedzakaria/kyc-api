package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.UserPrivilege;
import com.nexacore.systemmodule.privilege.entity.UserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, UserPrivilegeId> {
    void deleteByIdUserId(Long userId);
}
