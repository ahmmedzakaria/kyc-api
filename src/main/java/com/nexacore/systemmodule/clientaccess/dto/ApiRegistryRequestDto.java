package com.nexacore.systemmodule.clientaccess.dto;

import lombok.Data;

@Data
public class ApiRegistryRequestDto {
    private Long id;
    private String apiCode;
    private String httpMethod;
    private String pathPattern;
    private String moduleCode;
    private String moduleName;
    private String submoduleCode;
    private String submoduleName;
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private String requiredPrivilegeCode;
    private Boolean publicApi;
    private Boolean active;
}
