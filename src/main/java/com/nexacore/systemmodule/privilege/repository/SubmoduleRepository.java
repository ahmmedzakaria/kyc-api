package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.SysSubmodule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmoduleRepository extends JpaRepository<SysSubmodule, Long> {
    Optional<SysSubmodule> findByModuleCodeAndCode(String moduleCode, String code);
}
