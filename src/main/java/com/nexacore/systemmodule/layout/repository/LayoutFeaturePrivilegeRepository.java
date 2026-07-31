package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutFeaturePrivilege;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutFeaturePrivilegeRepository extends JpaRepository<SysLayoutFeaturePrivilege, Long> {
    List<SysLayoutFeaturePrivilege> findByLayoutFeatureIdAndActiveTrue(Long layoutFeatureId);
}
