package com.nexacore.authmodule.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
    private Long id;
    private String username;
    private Long personId;
    private String personName;
    private String email;
    private String mobile;
    private boolean enabled;
    private List<RoleDto> roles;
    private LocalDateTime lastLoginAt;
}
