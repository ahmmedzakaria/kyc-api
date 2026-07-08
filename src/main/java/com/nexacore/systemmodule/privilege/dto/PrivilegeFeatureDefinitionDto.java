package com.nexacore.systemmodule.privilege.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivilegeFeatureDefinitionDto {
    private String moduleCode;
    private String moduleName;
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private String menuLabel;
    private String icon;

    @Builder.Default
    private List<PrivilegeActionDefinitionDto> actions = new ArrayList<>();

    @Builder.Default
    private List<PrivilegeMenuItemDto> menuItems = new ArrayList<>();
}
