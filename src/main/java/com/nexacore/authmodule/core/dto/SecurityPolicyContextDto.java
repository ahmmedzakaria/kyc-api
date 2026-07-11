package com.nexacore.authmodule.core.dto;

import lombok.Builder;

@Builder
public record SecurityPolicyContextDto(
        boolean passwordLoginEnabled,
        boolean otpLoginEnabled,
        boolean ssoLoginEnabled,
        long sessionTimeoutSeconds,
        boolean refreshTokenEnabled,
        int maxLoginAttempts,
        String passwordPolicyCode
) {
}
