package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutProfileBranding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutProfileBrandingRepository extends JpaRepository<SysLayoutProfileBranding, Long> {
    Optional<SysLayoutProfileBranding> findFirstByLayoutProfileIdAndActiveTrueOrderByIdAsc(Long layoutProfileId);
}
