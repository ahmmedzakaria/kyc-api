package com.nexacore.systemmodule.publicnavigation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PublicNavigationScopeRequest(@NotNull Long tenantId, String clientCode, Set<String> surfaces) {}
