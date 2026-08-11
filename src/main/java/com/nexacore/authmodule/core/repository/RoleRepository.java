package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<AuthRole, Long> {
    Optional<AuthRole> findByName(String name);

    Optional<AuthRole> findByTenantIdAndNameIgnoreCase(Long tenantId, String name);

    Optional<AuthRole> findByTenantIdIsNullAndNameIgnoreCase(String name);

    Optional<AuthRole> findByIdAndTenantId(Long id, Long tenantId);

    Optional<AuthRole> findByIdAndTenantIdIsNull(Long id);

    Optional<AuthRole> findByTenantIdAndRoleCodeIgnoreCase(Long tenantId, String roleCode);

    Optional<AuthRole> findByTenantIdIsNullAndRoleCodeIgnoreCase(String roleCode);

    List<AuthRole> findByTenantIdIsNullOrTenantIdOrderByNameAsc(Long tenantId);
}
