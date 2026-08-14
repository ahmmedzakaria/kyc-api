package com.nexacore.authmodule.core.dto;

public record SecondFactorPolicyDto(String mode, String method) {
    public static SecondFactorPolicyDto disabled() {
        return new SecondFactorPolicyDto("DISABLED", null);
    }
}
