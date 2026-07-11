package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthClientAuthPolicyRepository extends JpaRepository<AuthClientAuthPolicy, Long> {
    List<AuthClientAuthPolicy> findByClientCodeIgnoreCase(String clientCode);
    List<AuthClientAuthPolicy> findByClientCodeIgnoreCaseAndEnabledTrue(String clientCode);
}
