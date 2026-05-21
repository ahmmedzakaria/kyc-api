package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
