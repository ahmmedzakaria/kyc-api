package com.nexacore.systemmodule.layout.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LayoutNavigationNodeRequestDto {
    private Long id;
    private Long parentId;
    private String code;
    private String tCode;
    private String name;
    private String categoryKind;
    private String route;
    private String icon;
    private String physicalModuleCode;
    private String physicalSubmoduleCode;
    private String physicalFeatureTypeCode;
    private String physicalFeatureCode;
    private Integer displayOrder;
    private Boolean active;
    private List<String> privilegeCodes = new ArrayList<>();
}
