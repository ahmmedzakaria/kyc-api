package com.nexacore.systemmodule.layout.repository;

import com.nexacore.systemmodule.layout.entity.SysClientLayoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ClientLayoutProfileRepository extends JpaRepository<SysClientLayoutProfile, Long> {
    List<SysClientLayoutProfile> findByTenantIdAndClientApplicationIdAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long tenantId, Long clientApplicationId);
    Optional<SysClientLayoutProfile> findFirstByTenantIdAndClientApplicationClientCodeAndDefaultProfileTrueAndActiveTrueOrderByDisplayOrderAscIdAsc(
            Long tenantId, String clientCode);
    boolean existsByTenantIdAndClientApplicationIdAndDefaultProfileTrueAndActiveTrueAndIdNot(
            Long tenantId, Long clientApplicationId, Long id);
    Optional<SysClientLayoutProfile> findByIdAndTenantId(Long id, Long tenantId);
    long countByTenantId(Long tenantId);

    @Query("""
            select count(assignment) from SysClientLayoutProfile assignment
            where assignment.tenantId = :tenantId
              and not exists (
                  select clientTenant.id from SysAccClientApplicationTenant clientTenant
                  where clientTenant.clientApplication.id = assignment.clientApplication.id
                    and clientTenant.tenantId = assignment.tenantId
                    and clientTenant.active = true
              )
            """)
    long countClientAssignmentMismatches(Long tenantId);
}
