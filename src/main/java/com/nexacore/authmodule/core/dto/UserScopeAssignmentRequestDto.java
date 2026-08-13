package com.nexacore.authmodule.core.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserScopeAssignmentRequestDto {
    private Long userId;
    private Set<UserScopeAssignmentDto> scopeAssignments;
    private String version;
}
