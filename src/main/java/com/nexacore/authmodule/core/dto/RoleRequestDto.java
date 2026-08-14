package com.nexacore.authmodule.core.dto;

import lombok.Data;

@Data
public class RoleRequestDto {
    private Long id;
    private Long tenantId;
    private String name;
    private String roleCode;
    private String description;
    private boolean active = true;
}
