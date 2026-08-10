package com.nexacore.systemmodule.backup.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BackupIdRequestDto(@NotNull UUID backupId) {}
