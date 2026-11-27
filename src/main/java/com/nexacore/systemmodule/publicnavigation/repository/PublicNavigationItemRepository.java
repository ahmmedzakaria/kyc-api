package com.nexacore.systemmodule.publicnavigation.repository;

import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PublicNavigationItemRepository extends JpaRepository<PublicNavigationItem, Long> {
    List<PublicNavigationItem> findByTenantIdAndNavigationGroupIdInAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long tenantId, Collection<Long> groupIds);
    List<PublicNavigationItem> findByTenantIdOrderByNavigationGroupIdAscDisplayOrderAscIdAsc(Long tenantId);
    Optional<PublicNavigationItem> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndParentId(Long tenantId, Long parentId);
    boolean existsByNavigationGroupIdAndItemCodeIgnoreCaseAndIdNot(Long groupId, String itemCode, Long id);
}
