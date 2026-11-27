package com.nexacore.systemmodule.publicnavigation.dto;

public record PublicNavigationGroupDto(Long id, Long tenantId, String groupCode, String groupName,
                                       String surface, Integer displayOrder, boolean active, Long version) {}
