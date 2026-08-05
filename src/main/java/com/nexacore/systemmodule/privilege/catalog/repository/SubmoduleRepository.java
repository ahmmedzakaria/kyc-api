package com.nexacore.systemmodule.privilege.catalog.repository;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivSubmodule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmoduleRepository extends JpaRepository<SysPrivSubmodule, Long> {
    Optional<SysPrivSubmodule> findByModuleCodeAndCode(String moduleCode, String code);
}
