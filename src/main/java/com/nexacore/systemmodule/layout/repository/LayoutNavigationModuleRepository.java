package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutNavigationModuleRepository extends JpaRepository<SysLayoutNavigationModule, Long> {
    List<SysLayoutNavigationModule> findByModuleGroupIdAndActiveTrueOrderByDisplayOrderAscNavigationModuleNameAsc(Long moduleGroupId);
}
