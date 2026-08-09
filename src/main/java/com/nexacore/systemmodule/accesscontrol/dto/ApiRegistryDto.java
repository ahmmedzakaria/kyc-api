package com.nexacore.systemmodule.accesscontrol.dto;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
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
    private String clientAuthenticationRequirement;
    private String userAuthorizationRequirement;
    private String dataScope;
    private String source;
    private int priority;
    private java.time.LocalDateTime lastSynchronizedAt;
    private boolean active;

    public static ApiRegistryDto fromEntity(SysAccApiRegistry api) {
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
                .clientAuthenticationRequirement(api.getClientAuthenticationRequirement())
                .userAuthorizationRequirement(api.getUserAuthorizationRequirement())
                .dataScope(api.getDataScope())
                .source(api.getSource())
                .priority(api.getPriority())
                .lastSynchronizedAt(api.getLastSynchronizedAt())
                .active(api.isActive())
                .build();
    }
}
