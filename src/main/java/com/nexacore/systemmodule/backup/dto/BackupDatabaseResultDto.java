package com.nexacore.systemmodule.backup.dto;

import com.nexacore.systemmodule.backup.enums.BackupJobStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BackupDatabaseResultDto(String logicalDatabase, String databaseName,
                                      BackupJobStatus status, LocalDateTime startedAt,
                                      LocalDateTime completedAt, Long sizeBytes, String sha256,
                                      String failureCode, String failureMessage) {}
