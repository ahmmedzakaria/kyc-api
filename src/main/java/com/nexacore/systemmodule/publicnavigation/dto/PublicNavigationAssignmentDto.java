package com.nexacore.systemmodule.publicnavigation.dto;

import java.util.Set;

public record PublicNavigationAssignmentDto(Long tenantId, String clientCode, Set<Long> itemIds) {}
