package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LayoutFeatureRepository extends JpaRepository<SysLayoutFeature, Long> {
    List<SysLayoutFeature> findByFeatureGroupIdAndActiveTrueOrderByDisplayOrderAscFeatureNameAsc(Long featureGroupId);

    List<SysLayoutFeature> findByActiveTrueOrderByDisplayOrderAscFeatureNameAsc();

    @Query("SELECT CASE WHEN COUNT(feature) > 0 THEN true ELSE false END "
            + "FROM SysLayoutFeature feature WHERE feature.tCode = :tCode AND feature.active = true")
    boolean existsActiveTCode(@Param("tCode") String tCode);

    @Query(value = "SELECT nextval('sys_layout_t_code_seq')", nativeQuery = true)
    long nextTCodeValue();
}
