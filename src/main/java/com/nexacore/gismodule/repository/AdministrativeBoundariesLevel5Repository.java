package com.nexacore.gismodule.repository;

import com.nexacore.gismodule.entity.AdministrativeBoundariesLevel5;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdministrativeBoundariesLevel5Repository extends JpaRepository<AdministrativeBoundariesLevel5, java.util.UUID> {

    @Query("""
        SELECT a FROM AdministrativeBoundariesLevel5 a
        WHERE
             LOWER(a.unionName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.subDistrictName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.districtName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(a.divisionName) LIKE LOWER(CONCAT('%', :search, '%'))
        """)
    Page<AdministrativeBoundariesLevel5> searchByAnyLevel(String search, Pageable pageable);
}

