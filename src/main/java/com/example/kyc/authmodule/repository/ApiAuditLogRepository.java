package com.example.kyc.authmodule.repository;

import com.example.kyc.authmodule.entity.ApiAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiAuditLogRepository extends JpaRepository<ApiAuditLog, Long> {
}

