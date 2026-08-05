package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientCredentialRepository extends JpaRepository<SysPrivClientCredential, Long> {
    List<SysPrivClientCredential> findByClientApplicationIdAndActiveTrue(Long clientApplicationId);

    Optional<SysPrivClientCredential> findByClientApplicationIdAndClientIdAndActiveTrue(Long clientApplicationId, String clientId);
}
