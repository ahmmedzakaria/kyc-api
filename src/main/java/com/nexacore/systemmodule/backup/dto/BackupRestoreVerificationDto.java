package com.nexacore.systemmodule.backup.dto;
import java.time.LocalDateTime;
import java.util.UUID;
public record BackupRestoreVerificationDto(Long id,UUID backupId,String status,String artifactSha256,LocalDateTime verifiedAt,
                                           String verifierId,String failureMessage,String offsiteReplicationStatus,String offsiteReference){}
