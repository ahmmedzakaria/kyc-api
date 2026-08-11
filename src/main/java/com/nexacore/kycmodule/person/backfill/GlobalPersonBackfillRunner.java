package com.nexacore.kycmodule.person.backfill;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(100)
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "nexacore.auth-person-backfill.enabled",
        havingValue = "true"
)
public class GlobalPersonBackfillRunner implements ApplicationRunner {
    private final GlobalPersonBackfillService backfillService;

    @Value("${nexacore.auth-person-backfill.page-size:250}")
    private int pageSize;

    @Override
    public void run(ApplicationArguments args) {
        GlobalPersonBackfillReport report = backfillService.backfill(pageSize);
        if (report.failed() > 0) {
            throw new IllegalStateException(
                    "Global person backfill completed with failures for person IDs " + report.failedPersonIds());
        }
    }
}
