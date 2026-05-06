package com.nexacore.kycmodule.repository;

import com.nexacore.kycmodule.entity.PersonDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonDetailsRepository extends JpaRepository<PersonDetails, Long> {
    PersonDetails findByPersonId(Long personId);
}

