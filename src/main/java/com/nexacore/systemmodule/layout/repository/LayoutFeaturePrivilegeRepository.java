package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutFeaturePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LayoutFeaturePrivilegeRepository extends JpaRepository<SysLayoutFeaturePrivilege, Long> {
    List<SysLayoutFeaturePrivilege> findByLayoutFeatureIdAndActiveTrue(Long layoutFeatureId);

    @Modifying(flushAutomatically = true)
    @Query("delete from SysLayoutFeaturePrivilege link where link.layoutFeature.id = :layoutFeatureId")
    void deleteByLayoutFeatureId(@Param("layoutFeatureId") Long layoutFeatureId);
}
