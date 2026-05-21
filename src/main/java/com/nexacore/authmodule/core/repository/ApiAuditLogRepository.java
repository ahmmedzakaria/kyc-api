package com.nexacore.authmodule.core.repository;

import com.nexacore.authmodule.core.entity.ApiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, Long> {
}

