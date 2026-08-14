package com.nexacore.systemmodule.dashboard.dto;
import java.time.LocalDateTime;
import java.util.Map;
public record DashboardAggregateDto(LocalDateTime generatedAt,Map<String,DashboardMetricDto> metrics,DashboardDiagnosticsDto diagnostics){}
