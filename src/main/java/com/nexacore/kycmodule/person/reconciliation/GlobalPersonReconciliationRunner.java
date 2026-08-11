package com.nexacore.kycmodule.person.reconciliation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "nexacore.auth-person-reconciliation.enabled", havingValue = "true")
public class GlobalPersonReconciliationRunner implements ApplicationRunner {
    private final GlobalPersonReconciliationService service;
    @Override public void run(ApplicationArguments args) {
        GlobalPersonReconciliationReport report = service.reconcile();
        log.info("Global person reconciliation: profileRefs={}, missingAuthIds={}, legacyRows={}, divergentLegacyIds={}",
                report.profilePersonIdsExamined(), report.missingAuthPersonIds(),
                report.legacyPersonsExamined(), report.legacyDivergentPersonIds());
        if (!report.missingAuthPersonIds().isEmpty()) {
            throw new IllegalStateException("KYC profiles reference missing Auth persons: " + report.missingAuthPersonIds());
        }
    }
}
