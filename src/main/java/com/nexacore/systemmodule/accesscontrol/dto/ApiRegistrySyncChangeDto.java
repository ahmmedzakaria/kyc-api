package com.nexacore.systemmodule.accesscontrol.dto;

public record ApiRegistrySyncChangeDto(String kind, String apiCode, ApiRegistryDto before, ApiRegistryDto after) {}
