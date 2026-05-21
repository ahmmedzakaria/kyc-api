package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.Privilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {
    Optional<Privilege> findByPrivilegeCode(String privilegeCode);

    List<Privilege> findByPrivilegeCodeIn(Collection<String> privilegeCodes);
}
