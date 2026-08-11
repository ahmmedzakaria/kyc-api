package com.nexacore.authmodule.core.entity;

import com.nexacore.commonmodule.dto.ActionInfo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "auth_roles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class AuthRole extends ActionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // ROLE_USER, ROLE_ADMIN

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "role_code", length = 100)
    private String roleCode;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Builder.Default
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy = 0L;

}
