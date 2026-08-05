package com.nexacore.systemmodule.privilege.accesscontrol.repository;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientApplicationRepository extends JpaRepository<SysPrivClientApplication, Long> {
    Optional<SysPrivClientApplication> findByClientCode(String clientCode);
}
