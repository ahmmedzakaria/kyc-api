package com.example.kyc.kycmodule.repository;

import com.example.kyc.kycmodule.entity.PersonDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonDetailsRepository extends JpaRepository<PersonDetails, Long> {
    PersonDetails findByPersonId(Long personId);
}

