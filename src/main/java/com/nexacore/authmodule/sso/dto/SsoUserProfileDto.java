package com.nexacore.authmodule.sso.dto;

public record SsoUserProfileDto(
        String subject,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified
) {
}
