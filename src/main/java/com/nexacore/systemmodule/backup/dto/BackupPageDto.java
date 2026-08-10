package com.nexacore.systemmodule.backup.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record BackupPageDto(List<BackupJobDto> items, long total, int page, int pageSize) {}
