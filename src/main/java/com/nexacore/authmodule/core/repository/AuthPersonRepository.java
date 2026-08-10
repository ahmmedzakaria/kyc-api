package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.AuthPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthPersonRepository extends JpaRepository<AuthPerson, Long> {
}
