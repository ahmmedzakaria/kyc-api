package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AuthUser, Long> {
    Optional<AuthUser> findByUsername(String username);
    Optional<AuthUser> findByEmail(String email);
    Optional<AuthUser> findByPersonId(Long personId);
    Optional<AuthUser> findByExternalProviderAndExternalSubject(String externalProvider, String externalSubject);

}
