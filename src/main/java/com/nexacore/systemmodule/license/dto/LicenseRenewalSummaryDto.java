package com.nexacore.systemmodule.license.dto;
import java.time.LocalDateTime;
public record LicenseRenewalSummaryDto(String subscriptionCode, String status, LocalDateTime expiresAt,
                                       LocalDateTime gracePeriodEndsAt, boolean autoRenew, long daysRemaining) {}
