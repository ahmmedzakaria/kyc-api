package com.nexacore.authmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivilegeActionDefinitionDto {
    private String actionCode;
    private String actionName;
    private String privilegeCode;
}
