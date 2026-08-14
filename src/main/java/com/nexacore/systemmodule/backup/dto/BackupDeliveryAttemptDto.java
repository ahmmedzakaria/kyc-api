package com.nexacore.systemmodule.backup.dto;
import java.time.LocalDateTime;
public record BackupDeliveryAttemptDto(Long id,String channel,String status,String attemptedBy,LocalDateTime attemptedAt,String failureCode,String failureMessage){}
