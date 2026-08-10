package com.nexacore.gatewaymodule.identity.dto;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GlobalPersonIdentityDto(
        Long personId,
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String bloodGroup,
        String primaryEmail,
        String primaryMobile,
        boolean emailVerified,
        boolean mobileVerified,
        boolean active
) {
}
