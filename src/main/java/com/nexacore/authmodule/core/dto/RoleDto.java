package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.entity.AuthRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDto {
    private Long id;
    private String name;
    private Long tenantId;
    private String roleCode;
    private String description;
    private boolean active;
    private boolean globalTemplate;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoleDto fromEntity(AuthRole role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .tenantId(role.getTenantId())
                .roleCode(role.getRoleCode())
                .description(role.getDescription())
                .active(role.isActive())
                .globalTemplate(role.getTenantId() == null)
                .createdBy(role.getCreatedBy())
                .updatedBy(role.getUpdatedBy())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
