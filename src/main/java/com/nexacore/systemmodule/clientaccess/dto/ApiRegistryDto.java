package com.nexacore.systemmodule.clientaccess.dto;

import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiRegistryDto {
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
    private boolean publicApi;
    private boolean active;

    public static ApiRegistryDto fromEntity(SysApiRegistry api) {
        return ApiRegistryDto.builder()
                .id(api.getId())
                .apiCode(api.getApiCode())
                .httpMethod(api.getHttpMethod())
                .pathPattern(api.getPathPattern())
                .moduleCode(api.getModuleCode())
                .moduleName(api.getModuleName())
                .submoduleCode(api.getSubmoduleCode())
                .submoduleName(api.getSubmoduleName())
                .featureTypeCode(api.getFeatureTypeCode())
                .featureTypeName(api.getFeatureTypeName())
                .featureCode(api.getFeatureCode())
                .featureName(api.getFeatureName())
                .actionCode(api.getActionCode())
                .actionName(api.getActionName())
                .requiredPrivilegeCode(api.getRequiredPrivilegeCode())
                .publicApi(api.isPublicApi())
                .active(api.isActive())
                .build();
    }
}
