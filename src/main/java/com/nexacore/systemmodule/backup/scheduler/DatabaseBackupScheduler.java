package com.nexacore.systemmodule.backup.scheduler;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import com.nexacore.systemmodule.backup.service.BackupJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupScheduler {
    private final BackupProperties properties;
    private final BackupJobService service;

    @Scheduled(cron = "${nexacore.backup.cron:0 0 2 * * *}", zone = "${nexacore.backup.zone:UTC}")
    public void schedule() {
        if (!properties.isEnabled()) return;
        try { service.enqueue(BackupTrigger.SCHEDULED, "system"); }
        catch (IllegalStateException exception) { log.warn("Scheduled database backup skipped: {}", exception.getMessage()); }
    }
}
