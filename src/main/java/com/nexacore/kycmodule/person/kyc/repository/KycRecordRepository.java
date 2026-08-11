package com.nexacore.kycmodule.person.kyc.repository;

import com.nexacore.kycmodule.person.kyc.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long>, JpaSpecificationExecutor<KycRecord> {
   // boolean existsByNationalId(String nationalId);

    Optional<KycRecord> findByIdAndTenantId(Long id, Long tenantId);

   // Page<KycRecord> findByNationalId(String nationalId, Pageable pageable);
}
