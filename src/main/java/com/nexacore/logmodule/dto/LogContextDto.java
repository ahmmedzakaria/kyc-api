package com.nexacore.logmodule.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogContextDto {
    private String traceId;
    private String clientCode;
    private String clientType;
    private Long userId;
    private String apiCode;
    private String moduleCode;
    private String moduleName;
    private String submoduleCode;
    private String submoduleName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private String accessMode;
    private String decision;
    private String denyReason;
    private Long businessId;
    private Long branchId;
}
