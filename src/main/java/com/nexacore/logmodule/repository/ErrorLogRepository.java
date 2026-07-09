package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.LogErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ErrorLogRepository extends JpaRepository<LogErrorLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
