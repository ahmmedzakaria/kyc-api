package com.nexacore.systemmodule.layout.dto;

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
public class NavNodeDto {
    private Long id;
    private Long version;
    private Long parentId;
    private String code;
    private String tCode;
    private String label;
    private String type;
    private String categoryKind;
    private String featureTypeCode;
    private String icon;
    private String moduleGroupIconName;
    private String moduleIconName;
    private String description;
    private String route;
    private String modulePrivCode;
    private String physicalModuleCode;
    private String physicalSubmoduleCode;
    private String physicalFeatureTypeCode;
    private String physicalFeatureCode;
    private Boolean navigationEnabled;
    private Boolean active;
    private Integer displayOrder;

    @Builder.Default
    private List<String> privilegeCodes = new ArrayList<>();

    @Builder.Default
    private List<NavNodeDto> children = new ArrayList<>();
}
