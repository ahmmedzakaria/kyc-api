package com.nexacore.kycmodule.person.reconciliation;

import java.util.List;

public record GlobalPersonReconciliationReport(long profilePersonIdsExamined,
        List<Long> missingAuthPersonIds, long legacyPersonsExamined,
        List<Long> legacyDivergentPersonIds) {
}
