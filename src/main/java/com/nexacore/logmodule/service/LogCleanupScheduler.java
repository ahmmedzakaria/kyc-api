package com.nexacore.logmodule.service;

import com.nexacore.logmodule.repository.ApiAccessLogRepository;
import com.nexacore.logmodule.repository.AuditLogRepository;
import com.nexacore.logmodule.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LogCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogCleanupScheduler.class);

    private final ApiAccessLogRepository apiAccessLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final boolean enabled;
    private final int retentionDays;

    public LogCleanupScheduler(ApiAccessLogRepository apiAccessLogRepository,
                               AuditLogRepository auditLogRepository,
                               ErrorLogRepository errorLogRepository,
                               @Value("${log.cleanup.enabled:true}") boolean enabled,
                               @Value("${log.cleanup.retention-days:90}") int retentionDays) {
        this.apiAccessLogRepository = apiAccessLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.enabled = enabled;
        this.retentionDays = retentionDays;
    }

    @Scheduled(cron = "${log.cleanup.cron:0 0 2 * * *}")
    @Transactional(transactionManager = "logTransactionManager")
    public void cleanupOldLogs() {
        if (!enabled) {
            return;
        }

        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(retentionDays);

        long apiAccessDeleted = apiAccessLogRepository.deleteByCreatedAtBefore(cutoffDateTime);
        long auditDeleted = auditLogRepository.deleteByCreatedAtBefore(cutoffDateTime);
        long errorDeleted = errorLogRepository.deleteByCreatedAtBefore(cutoffDateTime);

        LOGGER.info(
                "Log cleanup completed. cutoff={}, apiAccessDeleted={}, auditDeleted={}, errorDeleted={}",
                cutoffDateTime,
                apiAccessDeleted,
                auditDeleted,
                errorDeleted
        );
    }
}
