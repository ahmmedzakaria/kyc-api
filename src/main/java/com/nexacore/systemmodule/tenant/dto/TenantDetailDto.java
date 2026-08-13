package com.nexacore.systemmodule.tenant.dto;

import com.nexacore.systemmodule.tenant.entity.TenantStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TenantDetailDto(Long id, long version, String tenantCode, String displayName,
                              String legalName, String registrationNumber, String billingEmail,
                              String defaultLocale, String defaultTimeZone, TenantStatus status,
                              LocalDateTime activatedAt, LocalDateTime suspendedAt, String suspensionReason,
                              LocalDateTime cancelledAt, String cancellationReason,
                              List<TenantDomainDto> domains, TenantAssignmentSummaryDto assignments,
                              List<PlatformAdminAuditEventDto> history,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {}
