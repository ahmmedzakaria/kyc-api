package com.nexacore.kycmodule.person.api;

import java.time.Instant;

public record PersonRegisteredEvent(
        Long personId,
        String username,
        Instant occurredAt
) {
}

