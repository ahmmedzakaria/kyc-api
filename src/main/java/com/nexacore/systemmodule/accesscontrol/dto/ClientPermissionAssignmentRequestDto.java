package com.nexacore.systemmodule.accesscontrol.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ClientPermissionAssignmentRequestDto {
    private Long clientApplicationId;
    private String clientCode;
    private Set<Long> apiRegistryIds;
    private Set<String> privilegeCodes;
    private Set<Long> tenantIds;
    private Set<Long> businessIds;
}
