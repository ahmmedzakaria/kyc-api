package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutProfileSizes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LayoutProfileSizesRepository extends JpaRepository<SysLayoutProfileSizes, Long> {
    Optional<SysLayoutProfileSizes> findFirstByLayoutProfileIdAndActiveTrue(Long layoutProfileId);
}
