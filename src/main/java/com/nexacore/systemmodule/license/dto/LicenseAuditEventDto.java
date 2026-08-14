package com.nexacore.systemmodule.license.dto;
import java.time.LocalDateTime;
public record LicenseAuditEventDto(Long id, String subscriptionCode, String eventType, String eventMessageCode,
                                   Long actorUserId, String safeContextJson, LocalDateTime createdAt) {}
