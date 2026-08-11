package com.nexacore.systemmodule.tenant.repository;

import com.nexacore.systemmodule.tenant.entity.SysPlatformAdminAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdminAuditRepository extends JpaRepository<SysPlatformAdminAuditEvent, Long> {
}
