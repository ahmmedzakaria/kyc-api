package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutThemeChromeOverrides;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutThemeChromeOverridesRepository extends JpaRepository<SysLayoutThemeChromeOverrides, Long> {
    Optional<SysLayoutThemeChromeOverrides> findFirstByLayoutProfileThemeIdAndActiveTrue(Long layoutProfileThemeId);
    void deleteByLayoutProfileThemeId(Long layoutProfileThemeId);
}
