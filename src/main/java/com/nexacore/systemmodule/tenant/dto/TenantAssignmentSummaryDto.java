package com.nexacore.systemmodule.tenant.dto;

public record TenantAssignmentSummaryDto(long clients, long users, long layouts,
                                         long licenses, long brandingOverrides) {}
