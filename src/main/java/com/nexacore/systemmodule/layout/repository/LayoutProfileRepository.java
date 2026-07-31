package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LayoutProfileRepository extends JpaRepository<SysLayoutProfile, Long> {
    Optional<SysLayoutProfile> findByProfileCode(String profileCode);
    List<SysLayoutProfile> findByActiveTrueOrderByProfileCodeAsc();
}
