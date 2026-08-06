package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubMenuRepository extends JpaRepository<SysPrivSubMenu, Long> {
    @Query("""
            SELECT menu
            FROM SysPrivSubMenu menu
            JOIN menu.feature feature
            JOIN feature.submodule submodule
            JOIN submodule.module module
            WHERE module.code = :moduleCode
              AND submodule.code = :submoduleCode
              AND feature.featureType.featureTypeCode = :featureTypeCode
              AND feature.featureCode = :featureCode
              AND menu.url = :url
            """)
    Optional<SysPrivSubMenu> findFirstByFeatureAndUrl(
            @Param("moduleCode") String moduleCode,
            @Param("submoduleCode") String submoduleCode,
            @Param("featureTypeCode") String featureTypeCode,
            @Param("featureCode") String featureCode,
            @Param("url") String url
    );

    @Query("""
            SELECT menu
            FROM SysPrivSubMenu menu
            JOIN menu.feature feature
            JOIN feature.submodule submodule
            JOIN submodule.module module
            WHERE menu.active = true
            ORDER BY module.code ASC,
                     submodule.code ASC,
                     feature.featureType.featureTypeCode ASC,
                     feature.featureCode ASC,
                     menu.name ASC
            """)
    List<SysPrivSubMenu> findActiveOrderedByFeature();
}
