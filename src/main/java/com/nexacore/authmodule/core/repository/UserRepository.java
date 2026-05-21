package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByExternalProviderAndExternalSubject(String externalProvider, String externalSubject);

}
