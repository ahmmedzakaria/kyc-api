package com.nexacore.systemmodule.clientaccess.repository;

import com.nexacore.systemmodule.clientaccess.entity.SysClientCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientCredentialRepository extends JpaRepository<SysClientCredential, Long> {
    List<SysClientCredential> findByClientApplicationIdAndActiveTrue(Long clientApplicationId);

    Optional<SysClientCredential> findByClientApplicationIdAndClientIdAndActiveTrue(Long clientApplicationId, String clientId);
}
