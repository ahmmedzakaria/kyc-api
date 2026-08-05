package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureRepository extends JpaRepository<SysPrivFeature, Long> {
    Optional<SysPrivFeature> findBySubmoduleModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndCode(
            String moduleCode,
            String submoduleCode,
            String featureTypeCode,
            String code
    );
}
