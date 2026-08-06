package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutUiPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LayoutUiPolicyRepository extends JpaRepository<SysLayoutUiPolicy, Long> {
    Optional<SysLayoutUiPolicy> findByClientApplicationIdAndActionCode(Long clientApplicationId, String actionCode);

    @Query(value = """
            SELECT policy.*
            FROM sys_layout_ui_policies policy
            LEFT JOIN sys_priv_client_applications client ON client.id = policy.client_application_id
            WHERE policy.active = true
              AND (policy.client_application_id IS NULL OR upper(client.client_code) = upper(:clientCode))
            ORDER BY policy.action_code, policy.client_application_id NULLS FIRST
            """, nativeQuery = true)
    List<SysLayoutUiPolicy> findEffectiveCandidates(@Param("clientCode") String clientCode);
}
