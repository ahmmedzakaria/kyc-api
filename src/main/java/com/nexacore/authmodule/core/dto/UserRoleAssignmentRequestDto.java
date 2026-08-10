package com.nexacore.authmodule.core.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class UserRoleAssignmentRequestDto {
    private Long userId;
    private Set<Long> roleIds = new HashSet<>();
}
