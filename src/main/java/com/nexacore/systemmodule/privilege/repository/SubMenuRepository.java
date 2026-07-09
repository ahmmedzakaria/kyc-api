package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.SysSubMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubMenuRepository extends JpaRepository<SysSubMenu, Long> {
    Optional<SysSubMenu> findFirstByModuleCodeAndSubmoduleCodeAndFeatureTypeCodeAndFeatureCodeAndUrl(
            String moduleCode,
            String submoduleCode,
            String featureTypeCode,
            String featureCode,
            String url
    );

    List<SysSubMenu> findByActiveTrueOrderByModuleCodeAscSubmoduleCodeAscFeatureTypeCodeAscFeatureCodeAscNameAsc();
}
