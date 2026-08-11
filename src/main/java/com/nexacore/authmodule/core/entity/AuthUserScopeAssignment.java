package com.nexacore.authmodule.core.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_user_scope_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AuthUserScopeAssignment extends ActionInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false,
                    insertable = false, updatable = false),
            @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id",
                    nullable = false, insertable = false, updatable = false)
    })
    @EqualsAndHashCode.Exclude
    private AuthUser user;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "business_id")
    private Long businessId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @PrePersist
    @PreUpdate
    void synchronizeAccountKey() {
        if (user != null) {
            userId = user.getId();
            if (tenantId == null) {
                tenantId = user.getTenantId();
            } else if (!tenantId.equals(user.getTenantId())) {
                throw new IllegalStateException("Scope tenant must match the account tenant");
            }
        }
        if (userId == null || tenantId == null) {
            throw new IllegalStateException("Scope account and tenant are required");
        }
    }
}
