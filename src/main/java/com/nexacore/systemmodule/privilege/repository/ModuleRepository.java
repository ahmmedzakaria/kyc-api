package com.nexacore.systemmodule.privilege.repository;

import com.nexacore.systemmodule.privilege.entity.SysModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModuleRepository extends JpaRepository<SysModule, Long> {
    Optional<SysModule> findByCode(String code);
}
