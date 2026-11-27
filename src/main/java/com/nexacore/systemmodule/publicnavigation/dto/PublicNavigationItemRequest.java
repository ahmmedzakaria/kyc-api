package com.nexacore.systemmodule.publicnavigation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PublicNavigationItemRequest(Long id, @NotNull Long tenantId,
                                          @NotNull Long navigationGroupId, Long parentId,
                                          @NotBlank String itemCode, @NotBlank String itemType,
                                          @NotBlank String label, String description, String ariaLabel,
                                          String routePath, String externalUrl, String target, String icon,
                                          Integer displayOrder, Boolean active, Long version) {}
