package com.nexacore.systemmodule.backup.service;

import com.nexacore.systemmodule.backup.config.BackupProperties;
import com.nexacore.systemmodule.backup.dto.*;
import com.nexacore.systemmodule.backup.entity.SysBackupDatabaseResult;
import com.nexacore.systemmodule.backup.entity.SysBackupJob;
import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import com.nexacore.systemmodule.backup.repository.BackupJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

@Service
public class BackupJobService {
    private static final Set<BackupJobStatus> ACTIVE = Set.of(BackupJobStatus.QUEUED, BackupJobStatus.RUNNING);
    private final BackupJobRepository repository;
    private final BackupWorker worker;
    private final BackupProperties properties;
    private final Executor executor;

    public BackupJobService(BackupJobRepository repository, BackupWorker worker,
                            BackupProperties properties,
                            @Qualifier("backupExecutor") Executor executor) {
        this.repository = repository;
        this.worker = worker;
        this.properties = properties;
        this.executor = executor;
    }

    public synchronized BackupJobDto enqueue(BackupTrigger trigger, String requestedBy) {
        if (!properties.isEnabled()) throw new IllegalStateException("Database backup is disabled");
        if (repository.existsByStatusIn(ACTIVE)) throw new IllegalStateException("A database backup is already queued or running");
        SysBackupJob job = new SysBackupJob();
        job.setId(UUID.randomUUID());
        job.setTriggerType(trigger);
        job.setStatus(BackupJobStatus.QUEUED);
        job.setRequestedBy(requestedBy == null || requestedBy.isBlank() ? "system" : requestedBy);
        job.setRequestedAt(LocalDateTime.now());
        repository.save(job);
        executor.execute(() -> worker.execute(job.getId()));
        return toDto(job);
    }

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public BackupPageDto list(BackupListRequestDto request) {
        int page = request == null || request.page() == null ? 0 : Math.max(0, request.page());
        int pageSize = request == null || request.pageSize() == null ? 20 : Math.min(100, Math.max(1, request.pageSize()));
        var result = repository.findAllByOrderByRequestedAtDesc(PageRequest.of(page, pageSize));
        return BackupPageDto.builder().items(result.getContent().stream().map(this::toDto).toList())
                .total(result.getTotalElements()).page(page).pageSize(pageSize).build();
    }

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public BackupJobDto detail(UUID id) { return toDto(required(id)); }

    public FileSystemResource artifact(UUID id) {
        SysBackupJob job = required(id);
        if (job.getStatus() != BackupJobStatus.COMPLETED && job.getStatus() != BackupJobStatus.PARTIAL) {
            throw new IllegalStateException("Backup artifact is not available");
        }
        if (job.getArtifactName() == null) throw new IllegalStateException("Backup artifact is missing");
        Path root = properties.getLocalRoot().toAbsolutePath().normalize();
        Path artifact = root.resolve(job.getArtifactName()).normalize();
        if (!artifact.startsWith(root) || !Files.isRegularFile(artifact)) throw new IllegalStateException("Backup artifact is missing");
        return new FileSystemResource(artifact);
    }

    public String downloadName(UUID id) { return "nexacore-backup-" + id + ".zip.aesgcm"; }

    private SysBackupJob required(UUID id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Backup job not found: " + id));
    }

    private BackupJobDto toDto(SysBackupJob job) {
        List<BackupDatabaseResultDto> databases = job.getDatabaseResults() == null ? List.of()
                : job.getDatabaseResults().stream().map(this::toDto).toList();
        return BackupJobDto.builder().id(job.getId()).trigger(job.getTriggerType()).status(job.getStatus())
                .requestedBy(job.getRequestedBy()).requestedAt(job.getRequestedAt())
                .startedAt(job.getStartedAt()).completedAt(job.getCompletedAt())
                .artifactSizeBytes(job.getArtifactSizeBytes()).artifactSha256(job.getArtifactSha256())
                .expiresAt(job.getExpiresAt()).failureCode(job.getFailureCode())
                .failureMessage(job.getSanitizedFailureMessage()).databases(databases).build();
    }

    private BackupDatabaseResultDto toDto(SysBackupDatabaseResult result) {
        return BackupDatabaseResultDto.builder().logicalDatabase(result.getLogicalDatabase())
                .databaseName(result.getDatabaseName()).status(result.getStatus())
                .startedAt(result.getStartedAt()).completedAt(result.getCompletedAt())
                .sizeBytes(result.getSizeBytes()).sha256(result.getSha256())
                .failureCode(result.getFailureCode()).failureMessage(result.getSanitizedFailureMessage()).build();
    }
}
