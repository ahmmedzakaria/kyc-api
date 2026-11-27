package com.nexacore.systemmodule.publicnavigation.repository;

import com.nexacore.systemmodule.publicnavigation.entity.ClientPublicNavigationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientPublicNavigationItemRepository extends JpaRepository<ClientPublicNavigationItem, Long> {
    List<ClientPublicNavigationItem> findByTenantIdAndClientApplicationIdAndActiveTrue(
            Long tenantId, Long clientApplicationId);
    List<ClientPublicNavigationItem> findByTenantIdAndClientApplicationId(Long tenantId, Long clientApplicationId);
    Optional<ClientPublicNavigationItem> findByTenantIdAndClientApplicationIdAndPublicNavigationItemId(
            Long tenantId, Long clientApplicationId, Long itemId);
    boolean existsByTenantIdAndPublicNavigationItemId(Long tenantId, Long itemId);
}
