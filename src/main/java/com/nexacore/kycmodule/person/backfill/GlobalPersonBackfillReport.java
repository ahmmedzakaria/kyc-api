package com.nexacore.kycmodule.person.backfill;

import java.util.List;

public record GlobalPersonBackfillReport(
        long examined,
        long created,
        long updated,
        long failed,
        List<Long> failedPersonIds
) {
}
