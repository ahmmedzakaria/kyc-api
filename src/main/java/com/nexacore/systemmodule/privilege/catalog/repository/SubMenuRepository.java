package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysSubMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubMenuRepository extends JpaRepository<SysSubMenu, Long> {
    @Query("""
            SELECT menu
            FROM SysSubMenu menu
            JOIN menu.feature feature
            JOIN feature.submodule submodule
            JOIN submodule.module module
            WHERE module.code = :moduleCode
              AND submodule.code = :submoduleCode
              AND feature.featureTypeCode = :featureTypeCode
              AND feature.code = :featureCode
              AND menu.url = :url
            """)
    Optional<SysSubMenu> findFirstByFeatureAndUrl(
            @Param("moduleCode") String moduleCode,
            @Param("submoduleCode") String submoduleCode,
            @Param("featureTypeCode") String featureTypeCode,
            @Param("featureCode") String featureCode,
            @Param("url") String url
    );

    @Query("""
            SELECT menu
            FROM SysSubMenu menu
            JOIN menu.feature feature
            JOIN feature.submodule submodule
            JOIN submodule.module module
            WHERE menu.active = true
            ORDER BY module.code ASC,
                     submodule.code ASC,
                     feature.featureTypeCode ASC,
                     feature.code ASC,
                     menu.name ASC
            """)
    List<SysSubMenu> findActiveOrderedByFeature();
}
