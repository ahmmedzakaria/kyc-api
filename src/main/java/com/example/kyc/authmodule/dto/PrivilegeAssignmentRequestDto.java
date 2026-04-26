package com.example.kyc.authmodule.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class PrivilegeAssignmentRequestDto {
    private Long roleId;
    private Long userId;
    private Set<String> privilegeCodes = new HashSet<>();
}
