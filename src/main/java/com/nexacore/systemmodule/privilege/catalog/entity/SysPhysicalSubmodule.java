package com.nexacore.systemmodule.privilege.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sys_physical_submodules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sys_physical_submodules_module_code",
                columnNames = {"physical_module_id", "submodule_code"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysPhysicalSubmodule extends PhysicalCatalogAuditInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "physical_module_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPhysicalModule physicalModule;

    @Column(name = "submodule_code", nullable = false, length = 80)
    private String submoduleCode;

    @Column(name = "submodule_name", nullable = false)
    private String submoduleName;

    @Column(nullable = false)
    private boolean active;
}
