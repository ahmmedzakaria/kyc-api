package com.nexacore.gismodule.repository;

import com.nexacore.gismodule.entity.GisAdministrativeBoundariesLevel6;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdministrativeBoundariesLevel6Repository extends JpaRepository<GisAdministrativeBoundariesLevel6, java.util.UUID> {

    @Query("""
        SELECT a FROM GisAdministrativeBoundariesLevel6 a
        WHERE 
            LOWER(a.villageName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.unionName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.subDistrictName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.districtName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.divisionName) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<GisAdministrativeBoundariesLevel6> searchByAnyLevel(String search, Pageable pageable);
}

