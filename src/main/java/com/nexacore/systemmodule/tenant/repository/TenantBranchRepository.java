package com.nexacore.systemmodule.tenant.repository;
import com.nexacore.systemmodule.tenant.entity.SysTenantBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TenantBranchRepository extends JpaRepository<SysTenantBranch,Long> {
 List<SysTenantBranch> findByTenantIdAndBusinessIdAndActiveTrueOrderByDisplayNameAsc(Long tenantId,Long businessId);
 List<SysTenantBranch> findByTenantIdAndBusinessIdAndIdInAndActiveTrueOrderByDisplayNameAsc(Long tenantId,Long businessId,java.util.Collection<Long> ids);
 boolean existsByIdAndTenantIdAndBusinessIdAndActiveTrue(Long id,Long tenantId,Long businessId);
}
