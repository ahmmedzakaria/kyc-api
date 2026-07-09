package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<AuthRole, Long> {
    Optional<AuthRole> findByName(String name);
}
