package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRegistryRepository extends JpaRepository<SysPrivApiRegistry, Long> {
    Optional<SysPrivApiRegistry> findByApiCode(String apiCode);

    List<SysPrivApiRegistry> findByHttpMethodAndActiveTrue(String httpMethod);

    List<SysPrivApiRegistry> findBySource(String source);
}
