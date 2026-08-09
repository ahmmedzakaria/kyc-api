package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysLayoutRoutePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LayoutRoutePolicyRepository extends JpaRepository<SysLayoutRoutePolicy, Long> {
    Optional<SysLayoutRoutePolicy> findByClientApplicationIdAndRouteUrl(Long clientApplicationId, String routeUrl);

    @Query(value = """
            SELECT policy.*
            FROM sys_layout_route_policies policy
            LEFT JOIN sys_acc_client_applications client
              ON client.id = policy.client_application_id
            WHERE policy.active = true
              AND (policy.client_application_id IS NULL OR upper(client.client_code) = upper(:clientCode))
            ORDER BY policy.route_url, policy.client_application_id NULLS FIRST
            """, nativeQuery = true)
    List<SysLayoutRoutePolicy> findEffectiveCandidates(@Param("clientCode") String clientCode);
}
