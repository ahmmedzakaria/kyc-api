package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutProfileTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LayoutProfileThemeRepository extends JpaRepository<SysLayoutProfileTheme, Long> {
    List<SysLayoutProfileTheme> findByLayoutProfileIdAndActiveTrueOrderByIdAsc(Long layoutProfileId);
    List<SysLayoutProfileTheme> findByLayoutProfileId(Long layoutProfileId);
}
