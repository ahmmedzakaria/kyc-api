package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPersonProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PersonProfileRepository extends JpaRepository<KycPersonProfile, Long>, JpaSpecificationExecutor<KycPersonProfile> {
    long countByPersonIdAndActiveTrue(Long personId);
    @Query("select distinct profile.personId from KycPersonProfile profile order by profile.personId")
    List<Long> findDistinctPersonIds();
}
