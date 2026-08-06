package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPersonDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonDetailsRepository extends JpaRepository<KycPersonDetails, Long> {
    KycPersonDetails findByProfileId(Long profileId);
}
