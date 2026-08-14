package com.nexacore.systemmodule.backup.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record BackupReadinessDto(boolean backupEnabled,boolean encryptionConfigured,UUID latestSuccessfulBackupId,
                                 LocalDateTime latestSuccessfulAt,String latestChecksum,String restoreVerificationStatus,
                                 LocalDateTime restoreVerifiedAt,String offsiteReplicationStatus,boolean ready,String warningCode){}
