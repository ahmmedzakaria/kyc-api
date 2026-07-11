package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.RegistrationMode;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record RegistrationContextDto(
        boolean enabled,
        RegistrationMode mode,
        boolean requiresExistingPerson,
        boolean requiresApproval,
        boolean requiresInvite,
        boolean requiresEmailVerification,
        boolean requiresMobileVerification,
        List<String> allowedPersonTypes,
        List<String> requiredFields,
        List<String> requiredDocuments
) {
    public RegistrationContextDto {
        allowedPersonTypes = allowedPersonTypes == null ? new ArrayList<>() : allowedPersonTypes;
        requiredFields = requiredFields == null ? new ArrayList<>() : requiredFields;
        requiredDocuments = requiredDocuments == null ? new ArrayList<>() : requiredDocuments;
    }
}
