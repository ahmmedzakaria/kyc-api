package com.nexacore.systemmodule.layout.dto;

public record LayoutTenantReconciliationDto(long tenantId, long assignmentCount,
                                             long clientAssignmentMismatchCount,
                                             boolean consistent) {
}
