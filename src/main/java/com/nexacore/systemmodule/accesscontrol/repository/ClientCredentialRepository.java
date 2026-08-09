package com.nexacore.systemmodule.accesscontrol.repository;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClientCredentialRepository extends JpaRepository<SysAccClientCredential, Long> {
    List<SysAccClientCredential> findByClientApplicationIdAndActiveTrue(Long clientApplicationId);

    Optional<SysAccClientCredential> findByClientApplicationIdAndClientIdAndActiveTrue(Long clientApplicationId, String clientId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update SysAccClientCredential credential
               set credential.lastUsedAt = :usedAt,
                   credential.updatedAt = :usedAt
             where credential.id = :credentialId
               and (credential.lastUsedAt is null or credential.lastUsedAt < :cutoff)
            """)
    int updateLastUsedAtIfBefore(@Param("credentialId") Long credentialId,
                                 @Param("usedAt") LocalDateTime usedAt,
                                 @Param("cutoff") LocalDateTime cutoff);
}
