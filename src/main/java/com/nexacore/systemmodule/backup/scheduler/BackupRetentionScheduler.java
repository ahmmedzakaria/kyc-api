package com.nexacore.systemmodule.backup.scheduler;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BackupRetentionScheduler {
    private final BackupProperties properties;
    private final BackupJobRepository repository;

    @Scheduled(cron = "${nexacore.backup.retention-cron:0 30 3 * * *}", zone = "${nexacore.backup.zone:UTC}")
    public void removeExpired() {
        if (!properties.isEnabled()) return;
        List<SysBackupJob> completed = repository.findByStatusOrderByCompletedAtDesc(BackupJobStatus.COMPLETED);
        int protectedCount = Math.min(properties.getMinimumSuccessful(), completed.size());
        Path root = properties.getLocalRoot().toAbsolutePath().normalize();
        for (int index = protectedCount; index < completed.size(); index++) {
            SysBackupJob job = completed.get(index);
            if (job.getExpiresAt() == null || !job.getExpiresAt().isBefore(LocalDateTime.now()) || job.getArtifactName() == null) continue;
            Path artifact = root.resolve(job.getArtifactName()).normalize();
            if (!artifact.startsWith(root)) {
                log.error("Refusing to delete backup artifact outside configured root for job {}", job.getId());
                continue;
            }
            try {
                Files.deleteIfExists(artifact);
                job.setStatus(BackupJobStatus.DELETED);
                job.setArtifactName(null);
                repository.save(job);
            } catch (Exception exception) {
                log.error("Could not delete expired backup {}: {}", job.getId(), exception.getMessage());
            }
        }
    }
}
