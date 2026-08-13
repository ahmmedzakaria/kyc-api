package com.nexacore.systemmodule.privilege.catalog.dto;

import java.util.Map;

public record PrivilegeImpactDto(Long privilegeId, String privilegeCode, String version,
                                 Map<String,Long> activeDependencies, long totalActiveDependencies) {}
