package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<SysPrivFeature, Long> {
    @Query("""
            select feature
            from SysPrivFeature feature
            join fetch feature.submodule submodule
            join fetch submodule.module module
            join fetch feature.navigationCategory category
            where feature.active = true
              and submodule.active = true
              and module.active = true
            order by module.modulePrivCode, submodule.submodulePrivCode,
                     category.featureTypeCode, feature.featureCode
            """)
    List<SysPrivFeature> findActiveCatalogFeatures();

    Optional<SysPrivFeature> findBySubmoduleModuleModulePrivCodeAndSubmoduleSubmodulePrivCodeAndNavigationCategoryFeatureTypeCodeAndFeatureCode(
            String modulePrivCode,
            String submodulePrivCode,
            String featureTypeCode,
            String code
    );
}
