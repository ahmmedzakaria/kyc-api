package com.nexacore.systemmodule.backup.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record BackupDeliveryRetryRequestDto(@NotNull UUID backupId){}
