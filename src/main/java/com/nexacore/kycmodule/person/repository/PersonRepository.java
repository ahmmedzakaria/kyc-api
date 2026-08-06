package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<KycPerson, Long>, JpaSpecificationExecutor<KycPerson> {
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<KycPerson> findByUsername(String username);
    Optional<KycPerson> findByEmail(String email);
    Optional<KycPerson> findByMobileNumber(String mobileNumber);
}
