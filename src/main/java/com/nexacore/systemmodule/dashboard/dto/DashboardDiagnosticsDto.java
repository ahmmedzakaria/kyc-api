package com.nexacore.systemmodule.dashboard.dto;
import java.time.LocalDateTime;
public record DashboardDiagnosticsDto(String enforcementMode,boolean accessControlEnabled,LocalDateTime registryLastSynchronizedAt){}
