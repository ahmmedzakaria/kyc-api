package com.example.kyc.authmodule.dto;

import com.example.kyc.authmodule.entity.SubMenu;
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
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private boolean active;
    private Long createdBy;
    private Long updatedBy;

    public static SubMenuDto fromEntity(SubMenu subMenu) {
        return SubMenuDto.builder()
                .id(subMenu.getId())
                .name(subMenu.getName())
                .url(subMenu.getUrl())
                .icon(subMenu.getIcon())
                .moduleCode(subMenu.getModuleCode())
                .moduleName(subMenu.getModuleName())
                .featureTypeCode(subMenu.getFeatureTypeCode())
                .featureTypeName(subMenu.getFeatureTypeName())
                .featureCode(subMenu.getFeatureCode())
                .featureName(subMenu.getFeatureName())
                .active(subMenu.isActive())
                .createdBy(subMenu.getCreatedBy())
                .updatedBy(subMenu.getUpdatedBy())
                .build();
    }
}
