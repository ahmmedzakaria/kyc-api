package com.nexacore.gatewaymodule.workflow.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record WorkflowDecisionResponseDto(
        boolean allowed,
        String messageCode,
        String message,
        List<String> availableActionCodes
) {
}
