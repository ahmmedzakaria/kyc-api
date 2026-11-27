package com.nexacore.systemmodule.publicnavigation.repository;

import com.nexacore.systemmodule.publicnavigation.entity.PublicNavigationGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PublicNavigationGroupRepository extends JpaRepository<PublicNavigationGroup, Long> {
    List<PublicNavigationGroup> findByTenantIdAndSurfaceInAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long tenantId, Collection<String> surfaces);
    List<PublicNavigationGroup> findByTenantIdOrderBySurfaceAscDisplayOrderAscIdAsc(Long tenantId);
    Optional<PublicNavigationGroup> findByIdAndTenantId(Long id, Long tenantId);
    boolean existsByTenantIdAndGroupCodeIgnoreCaseAndIdNot(Long tenantId, String groupCode, Long id);
}
