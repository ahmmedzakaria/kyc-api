package com.nexacore.authmodule.repository;

import com.nexacore.authmodule.entity.SubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubMenuRepository extends JpaRepository<SubMenu, Long> {
    Optional<SubMenu> findFirstByModuleCodeAndFeatureTypeCodeAndFeatureCodeAndUrl(
            String moduleCode,
            String featureTypeCode,
            String featureCode,
            String url
    );

    List<SubMenu> findByActiveTrueOrderByModuleCodeAscFeatureTypeCodeAscFeatureCodeAscNameAsc();
}
