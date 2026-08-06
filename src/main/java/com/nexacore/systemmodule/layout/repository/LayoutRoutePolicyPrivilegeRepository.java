package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicyPrivilege;
import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicyPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LayoutRoutePolicyPrivilegeRepository extends JpaRepository<SysLayoutRoutePolicyPrivilege, SysLayoutRoutePolicyPrivilegeId> {
    List<SysLayoutRoutePolicyPrivilege> findByRoutePolicyIdAndActiveTrue(Long routePolicyId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SysLayoutRoutePolicyPrivilege link where link.id.routePolicyId = :routePolicyId")
    void deleteByRoutePolicyId(@Param("routePolicyId") Long routePolicyId);
}
