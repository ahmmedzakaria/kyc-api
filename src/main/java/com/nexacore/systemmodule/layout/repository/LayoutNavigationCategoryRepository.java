package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutNavigationCategoryRepository extends JpaRepository<SysLayoutNavigationCategory, Long> {
    List<SysLayoutNavigationCategory> findByNavigationModuleIdAndActiveTrueOrderByDisplayOrderAscCategoryNameAsc(Long navigationModuleId);
}
