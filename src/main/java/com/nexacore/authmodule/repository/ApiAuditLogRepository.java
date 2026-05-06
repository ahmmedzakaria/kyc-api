package com.nexacore.authmodule.repository;

import com.nexacore.authmodule.entity.ApiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, Long> {
}

