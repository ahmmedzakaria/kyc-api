package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutFeatureRepository extends JpaRepository<SysLayoutFeature, Long> {
    List<SysLayoutFeature> findByFeatureGroupIdAndActiveTrueOrderByDisplayOrderAscFeatureNameAsc(Long featureGroupId);
}
