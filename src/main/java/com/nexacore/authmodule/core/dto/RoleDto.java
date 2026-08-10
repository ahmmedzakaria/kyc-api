package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.entity.AuthRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private Long id;
    private String name;

    public static RoleDto fromEntity(AuthRole role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .build();
    }
}
