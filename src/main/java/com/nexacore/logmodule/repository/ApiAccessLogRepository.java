package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.LogApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;

public interface ApiAccessLogRepository extends JpaRepository<LogApiAccessLog, Long>, JpaSpecificationExecutor<LogApiAccessLog> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
