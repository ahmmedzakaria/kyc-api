package com.nexacore.authmodule.core.dto;

import lombok.Data;

@Data
public class PrivilegeRequestDto {
    private String moduleCode;
    private String moduleName;
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private Long subMenuId;
    private Boolean active;
}
