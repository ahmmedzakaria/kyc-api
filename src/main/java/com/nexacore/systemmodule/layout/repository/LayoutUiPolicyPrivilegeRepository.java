package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicyPrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicyPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LayoutUiPolicyPrivilegeRepository extends JpaRepository<SysLayoutUiPolicyPrivilege, SysLayoutUiPolicyPrivilegeId> {
    List<SysLayoutUiPolicyPrivilege> findByUiPolicyIdAndActiveTrue(Long uiPolicyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysLayoutUiPolicyPrivilege link where link.id.uiPolicyId = :uiPolicyId")
    void deleteByUiPolicyId(@Param("uiPolicyId") Long uiPolicyId);
}
