package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.LogAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<LogAuditLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
