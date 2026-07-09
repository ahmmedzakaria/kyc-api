package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.LogApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ApiAccessLogRepository extends JpaRepository<LogApiAccessLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
