package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutNavigationModuleRepository extends JpaRepository<SysPrivModule, Long> {
    List<SysPrivModule> findByModuleGroupIdAndActiveTrueOrderByDisplayOrderAscNameAsc(Long moduleGroupId);

    List<SysPrivModule> findByActiveTrueAndNavigationEnabledTrueOrderByDisplayOrderAscNameAsc();
}
