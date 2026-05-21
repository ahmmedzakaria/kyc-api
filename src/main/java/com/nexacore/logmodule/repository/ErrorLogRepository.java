package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
