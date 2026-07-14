package com.nexacore.systemmodule.privilege.accesscontrol.repository;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiRegistryRepository extends JpaRepository<SysApiRegistry, Long> {
    Optional<SysApiRegistry> findByApiCode(String apiCode);

    List<SysApiRegistry> findByHttpMethodAndActiveTrue(String httpMethod);
}
