package com.nexacore.systemmodule.publicnavigation.dto;

public record PublicNavigationItemDto(Long id, Long tenantId, Long navigationGroupId, Long parentId,
                                      String itemCode, String itemType, String label, String description,
                                      String ariaLabel, String routePath, String externalUrl, String target,
                                      String icon, Integer displayOrder, boolean active, Long version) {}
