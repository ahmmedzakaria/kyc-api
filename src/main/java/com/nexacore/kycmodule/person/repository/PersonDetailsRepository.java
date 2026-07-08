package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.PersonDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonDetailsRepository extends JpaRepository<PersonDetails, Long> {
    PersonDetails findByPersonId(Long personId);
}

