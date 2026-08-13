package com.nexacore.systemmodule.accesscontrol.dto;

import lombok.Builder;
import java.util.Set;

@Builder
public record ClientAdministrationDetailDto(
        ClientApplicationDto client,
        Set<Long> apiRegistryIds,
        Set<String> privilegeCodes,
        Set<ClientScopeAssignmentDto> scopeAssignments
) {}
