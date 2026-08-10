package com.nexacore.systemmodule.backup.dto;

import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import com.nexacore.systemmodule.backup.enums.BackupTrigger;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record BackupJobDto(UUID id, BackupTrigger trigger, BackupJobStatus status,
                           String requestedBy, LocalDateTime requestedAt,
                           LocalDateTime startedAt, LocalDateTime completedAt,
                           Long artifactSizeBytes, String artifactSha256,
                           LocalDateTime expiresAt, String failureCode,
                           String failureMessage, List<BackupDatabaseResultDto> databases) {}
