package com.nexacore.systemmodule.publicnavigation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PublicNavigationAssignmentRequest(@NotNull Long tenantId, @NotBlank String clientCode,
                                                @NotNull Set<Long> itemIds) {}
