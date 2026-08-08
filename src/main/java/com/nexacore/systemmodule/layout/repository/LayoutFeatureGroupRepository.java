package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutFeatureGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutFeatureGroupRepository extends JpaRepository<SysLayoutFeatureGroup, Long> {
    List<SysLayoutFeatureGroup> findByNavigationCategoryIdAndActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc(Long navigationCategoryId);

    List<SysLayoutFeatureGroup> findByActiveTrueOrderByDisplayOrderAscFeatureGroupNameAsc();
}
