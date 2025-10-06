package com.example.kyc.kycmodule.repository;

import com.example.kyc.kycmodule.entity.KycRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {
   // boolean existsByNationalId(String nationalId);

    Page<KycRecord> findByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContaining(String firstName, String lastName, Pageable pageable);

   // Page<KycRecord> findByNationalId(String nationalId, Pageable pageable);
}
