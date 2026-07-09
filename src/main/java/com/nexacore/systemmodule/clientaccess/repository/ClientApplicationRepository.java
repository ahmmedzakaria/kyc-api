package com.nexacore.systemmodule.clientaccess.repository;

import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientApplicationRepository extends JpaRepository<SysClientApplication, Long> {
    Optional<SysClientApplication> findByClientCode(String clientCode);
}
