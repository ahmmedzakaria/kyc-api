package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthClientAuthPolicyRepository extends JpaRepository<AuthClientAuthPolicy, Long> {
    Optional<AuthClientAuthPolicy> findByTenantIdAndClientCodeIgnoreCase(Long tenantId, String clientCode);
    Optional<AuthClientAuthPolicy> findByTenantIdAndClientCodeIgnoreCaseAndEnabledTrue(Long tenantId, String clientCode);
}
