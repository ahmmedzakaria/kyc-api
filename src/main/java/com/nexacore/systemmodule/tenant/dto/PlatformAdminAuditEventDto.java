package com.nexacore.systemmodule.tenant.dto;

import java.time.LocalDateTime;

public record PlatformAdminAuditEventDto(Long id, Long actorUserId, String actionCode, String outcome,
                                         String reason, String traceId, LocalDateTime createdAt) {}
