package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureTypeRepository extends JpaRepository<SysPrivFeatureType, Long> {
    Optional<SysPrivFeatureType> findByTenantIdIsNullAndFeatureTypeCode(String featureTypeCode);
    Optional<SysPrivFeatureType> findByTenantIdAndFeatureTypeCode(Long tenantId, String featureTypeCode);
}
