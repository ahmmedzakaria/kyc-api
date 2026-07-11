package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthClientRegistrationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthClientRegistrationPolicyRepository extends JpaRepository<AuthClientRegistrationPolicy, Long> {
    List<AuthClientRegistrationPolicy> findByClientCodeIgnoreCase(String clientCode);
    List<AuthClientRegistrationPolicy> findByClientCodeIgnoreCaseAndEnabledTrue(String clientCode);
}
