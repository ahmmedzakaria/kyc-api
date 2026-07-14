package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureRepository extends JpaRepository<SysFeature, Long> {
    Optional<SysFeature> findBySubmoduleModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndCode(
            String moduleCode,
            String submoduleCode,
            String featureTypeCode,
            String code
    );
}
