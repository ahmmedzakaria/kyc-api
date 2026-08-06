package com.nexacore.kycmodule.person.repository;

import com.nexacore.kycmodule.person.entity.KycPersonOrganizationMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonOrganizationMembershipRepository extends JpaRepository<KycPersonOrganizationMembership, Long> {
}
