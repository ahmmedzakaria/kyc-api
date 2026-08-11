package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.AssignmentScope;
import com.nexacore.systemmodule.layout.enums.DeviceTarget;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLayoutAssignmentDto {
    private Long id;
    private Long tenantId;
    private Long clientApplicationId;
    private String clientCode;
    private Long layoutProfileId;
    private String profileCode;
    private AssignmentScope assignmentScope;
    private String roleCode;
    private String privilegeCode;
    private DeviceTarget deviceTarget;
    private String moduleCode;
    private LayoutBrandDto brandOverride;
    private Boolean defaultProfile;
    private Boolean selectable;
    private Integer displayOrder;
    private Boolean active;
}
