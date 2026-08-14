package com.nexacore.systemmodule.license.repository;

import com.nexacore.systemmodule.license.entity.SysLicenseAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseAuditEventRepository extends JpaRepository<SysLicenseAuditEvent, Long> {
    java.util.List<SysLicenseAuditEvent> findTop100ByLicenseSubscriptionSubscriptionCodeAndTenantIdOrderByCreatedAtDesc(String subscriptionCode, Long tenantId);
}
