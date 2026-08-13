package com.nexacore.systemmodule.tenant.repository;
import com.nexacore.systemmodule.tenant.entity.SysTenantBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TenantBusinessRepository extends JpaRepository<SysTenantBusiness,Long> {
 List<SysTenantBusiness> findByTenantIdAndActiveTrueOrderByDisplayNameAsc(Long tenantId);
 List<SysTenantBusiness> findByTenantIdAndIdInAndActiveTrueOrderByDisplayNameAsc(Long tenantId, java.util.Collection<Long> ids);
 Optional<SysTenantBusiness> findByIdAndTenantIdAndActiveTrue(Long id,Long tenantId);
}
