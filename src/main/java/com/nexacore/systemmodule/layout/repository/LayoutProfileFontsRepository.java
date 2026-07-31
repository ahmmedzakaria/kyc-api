package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutProfileFonts;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutProfileFontsRepository extends JpaRepository<SysLayoutProfileFonts, Long> {
    Optional<SysLayoutProfileFonts> findFirstByLayoutProfileIdAndActiveTrue(Long layoutProfileId);
}
