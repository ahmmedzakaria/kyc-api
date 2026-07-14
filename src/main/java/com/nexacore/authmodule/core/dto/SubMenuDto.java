package com.nexacore.authmodule.core.dto;

import com.nexacore.systemmodule.privilege.catalog.entity.SysSubMenu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubMenuDto {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private String moduleCode;
    private String moduleName;
    private String submoduleCode;
    private String submoduleName;
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private boolean active;
    private Integer menuOrder;
    private Integer subMenuOrder;
    private Long createdBy;
    private Long updatedBy;

    public static SubMenuDto fromEntity(SysSubMenu subMenu) {
        return SubMenuDto.builder()
                .id(subMenu.getId())
                .name(subMenu.getName())
                .url(subMenu.getUrl())
                .icon(subMenu.getIcon())
                .moduleCode(subMenu.getModuleCode())
                .moduleName(subMenu.getModuleName())
                .submoduleCode(subMenu.getSubmoduleCode())
                .submoduleName(subMenu.getSubmoduleName())
                .featureTypeCode(subMenu.getFeatureTypeCode())
                .featureTypeName(subMenu.getFeatureTypeName())
                .featureCode(subMenu.getFeatureCode())
                .featureName(subMenu.getFeatureName())
                .active(subMenu.isActive())
                .menuOrder(subMenu.getMenuOrder())
                .subMenuOrder(subMenu.getSubMenuOrder())
                .createdBy(subMenu.getCreatedBy())
                .updatedBy(subMenu.getUpdatedBy())
                .build();
    }
}
