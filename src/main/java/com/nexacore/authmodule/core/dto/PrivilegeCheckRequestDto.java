package com.nexacore.authmodule.core.dto;

import lombok.Data;

@Data
public class PrivilegeCheckRequestDto {
    private String username;
    private String privilegeCode;
    private String moduleCode;
    private String featureTypeCode;
    private String featureCode;
    private String actionCode;
}
