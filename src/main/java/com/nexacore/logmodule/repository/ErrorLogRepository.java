package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.LogErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface ErrorLogRepository extends JpaRepository<LogErrorLog, Long>, JpaSpecificationExecutor<LogErrorLog> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
