package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.entity.Privilege;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivilegeDto {
    private Long id;
    private String privilegeCode;
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
    private Long subMenuId;
    private String subMenuName;
    private String subMenuUrl;
    private boolean active;

    public static PrivilegeDto fromEntity(Privilege privilege) {
        return PrivilegeDto.builder()
                .id(privilege.getId())
                .privilegeCode(privilege.getPrivilegeCode())
                .moduleCode(privilege.getModuleCode())
                .moduleName(privilege.getModuleName())
                .submoduleCode(privilege.getSubmoduleCode())
                .submoduleName(privilege.getSubmoduleName())
                .featureTypeCode(privilege.getFeatureTypeCode())
                .featureTypeName(privilege.getFeatureTypeName())
                .featureCode(privilege.getFeatureCode())
                .featureName(privilege.getFeatureName())
                .actionCode(privilege.getActionCode())
                .actionName(privilege.getActionName())
                .subMenuId(privilege.getSubMenu() == null ? null : privilege.getSubMenu().getId())
                .subMenuName(privilege.getSubMenu() == null ? null : privilege.getSubMenu().getName())
                .subMenuUrl(privilege.getSubMenu() == null ? null : privilege.getSubMenu().getUrl())
                .active(privilege.isActive())
                .build();
    }
}
