package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutThemePrimaries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutThemePrimariesRepository extends JpaRepository<SysLayoutThemePrimaries, Long> {
    Optional<SysLayoutThemePrimaries> findFirstByLayoutProfileThemeIdAndActiveTrue(Long layoutProfileThemeId);
    void deleteByLayoutProfileThemeId(Long layoutProfileThemeId);
}
