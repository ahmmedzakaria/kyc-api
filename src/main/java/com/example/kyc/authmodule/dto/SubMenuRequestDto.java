package com.example.kyc.authmodule.dto;

import lombok.Data;

@Data
public class SubMenuRequestDto {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private String moduleCode;
    private String moduleName;
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private Boolean active;
}
