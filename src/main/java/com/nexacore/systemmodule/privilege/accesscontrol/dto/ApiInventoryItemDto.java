package com.nexacore.systemmodule.privilege.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApiInventoryItemDto {
    private String apiCode;
    private String httpMethod;
    private String pathPattern;
    private String controllerClass;
    private String handlerMethod;
    private String sourceModule;
    private boolean accessMetadataDeclared;
    private Boolean publicApi;
    private String moduleCode;
    private String submoduleCode;
    private String featureTypeCode;
    private String featureCode;
    private String actionCode;
    private String requiredPrivilegeCode;

    @Builder.Default
    private List<String> intendedClientTypes = List.of();

    private String dataScope;
    private String reviewStatus;
}
