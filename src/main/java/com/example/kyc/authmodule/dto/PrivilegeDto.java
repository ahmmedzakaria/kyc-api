package com.example.kyc.authmodule.dto;

import com.example.kyc.authmodule.entity.Privilege;
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
    private String featureTypeCode;
    private String featureTypeName;
    private String featureCode;
    private String featureName;
    private String actionCode;
    private String actionName;
    private boolean active;

    public static PrivilegeDto fromEntity(Privilege privilege) {
        return PrivilegeDto.builder()
                .id(privilege.getId())
                .privilegeCode(privilege.getPrivilegeCode())
                .moduleCode(privilege.getModuleCode())
                .moduleName(privilege.getModuleName())
                .featureTypeCode(privilege.getFeatureTypeCode())
                .featureTypeName(privilege.getFeatureTypeName())
                .featureCode(privilege.getFeatureCode())
                .featureName(privilege.getFeatureName())
                .actionCode(privilege.getActionCode())
                .actionName(privilege.getActionName())
                .active(privilege.isActive())
                .build();
    }
}
