package com.nexacore.systemmodule.accesscontrol.security;

public record ClientRateLimitDecision(
        boolean allowed,
        long limit,
        long remaining,
        long retryAfterSeconds
) {
    public static ClientRateLimitDecision notLimited() {
        return new ClientRateLimitDecision(true, 0, Long.MAX_VALUE, 0);
    }
}
