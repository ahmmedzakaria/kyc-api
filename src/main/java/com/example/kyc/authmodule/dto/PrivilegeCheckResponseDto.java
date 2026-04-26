package com.example.kyc.authmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivilegeCheckResponseDto {
    private String username;
    private String privilegeCode;
    private boolean allowed;
}
