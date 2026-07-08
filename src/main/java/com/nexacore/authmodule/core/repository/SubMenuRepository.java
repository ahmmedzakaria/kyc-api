package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubMenuRepository extends JpaRepository<SubMenu, Long> {
    Optional<SubMenu> findFirstByModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndFeatureCodeAndUrl(
            String moduleCode,
            String submoduleCode,
            String featureTypeCode,
            String featureCode,
            String url
    );

    List<SubMenu> findByActiveTrueOrderByModuleCodeAscSubmoduleCodeAscFeatureTypeCodeAscFeatureCodeAscNameAsc();
}
