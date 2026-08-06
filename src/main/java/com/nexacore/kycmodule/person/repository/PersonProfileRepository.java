package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPersonProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PersonProfileRepository extends JpaRepository<KycPersonProfile, Long>, JpaSpecificationExecutor<KycPersonProfile> {
    long countByPersonIdAndActiveTrue(Long personId);
}
