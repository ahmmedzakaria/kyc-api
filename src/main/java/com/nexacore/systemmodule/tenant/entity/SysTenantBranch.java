package com.nexacore.systemmodule.tenant.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="sys_tenant_branches") @Getter @Setter
public class SysTenantBranch extends ActionInfo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="tenant_id",nullable=false) private Long tenantId;
    @Column(name="business_id",nullable=false) private Long businessId;
    @Column(name="branch_code",nullable=false) private String branchCode;
    @Column(name="display_name",nullable=false) private String displayName;
    @Column(nullable=false) private boolean active=true;
    @Column(name="created_by",nullable=false) private Long createdBy=0L;
    @Column(name="updated_by",nullable=false) private Long updatedBy=0L;
}
