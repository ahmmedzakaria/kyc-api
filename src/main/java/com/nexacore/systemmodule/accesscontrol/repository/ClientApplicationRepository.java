package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientApplicationRepository extends JpaRepository<SysAccClientApplication, Long> {
    Optional<SysAccClientApplication> findByClientCode(String clientCode);
}
