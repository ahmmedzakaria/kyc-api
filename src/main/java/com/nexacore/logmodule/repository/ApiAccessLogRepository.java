package com.nexacore.logmodule.repository;

import com.nexacore.logmodule.entity.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {
    long deleteByCreatedAtBefore(LocalDateTime cutoffDateTime);
}
