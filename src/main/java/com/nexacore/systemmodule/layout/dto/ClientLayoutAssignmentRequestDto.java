package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.AssignmentScope;
import com.nexacore.systemmodule.layout.enums.DeviceTarget;
import lombok.Data;

@Data
public class ClientLayoutAssignmentRequestDto {
    private Long id;
    private Long clientApplicationId;
    private String clientCode;
    private Long layoutProfileId;
    private String profileCode;
    private AssignmentScope assignmentScope;
    private String roleCode;
    private String privilegeCode;
    private DeviceTarget deviceTarget;
    private String moduleCode;
    private Boolean defaultProfile;
    private Boolean selectable;
    private Integer displayOrder;
    private Boolean active;
}
