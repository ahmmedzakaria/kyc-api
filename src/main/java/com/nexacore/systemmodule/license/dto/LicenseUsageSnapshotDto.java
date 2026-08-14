package com.nexacore.systemmodule.license.dto;
import java.time.LocalDateTime;
public record LicenseUsageSnapshotDto(Long id, String subscriptionCode, String usagePeriod, String usageCode,
                                      Long usageValue, LocalDateTime measuredAt) {}
