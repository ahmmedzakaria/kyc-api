package com.nexacore.systemmodule.publicnavigation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PublicNavigationGroupRequest(Long id, @NotNull Long tenantId, @NotBlank String groupCode,
                                           @NotBlank String groupName, @NotBlank String surface,
                                           Integer displayOrder, Boolean active, Long version) {}
