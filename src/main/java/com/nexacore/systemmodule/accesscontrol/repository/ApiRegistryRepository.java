package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRegistryRepository extends JpaRepository<SysAccApiRegistry, Long> {
    Optional<SysAccApiRegistry> findByApiCode(String apiCode);

    List<SysAccApiRegistry> findByHttpMethodAndActiveTrue(String httpMethod);

    List<SysAccApiRegistry> findBySource(String source);
}
