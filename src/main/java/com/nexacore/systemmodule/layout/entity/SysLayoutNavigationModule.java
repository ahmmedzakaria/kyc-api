package com.nexacore.systemmodule.layout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_navigation_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutNavigationModule extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_group_id", nullable = false)
    private SysLayoutModuleGroup moduleGroup;

    @Column(name = "navigation_module_code", nullable = false, unique = true, length = 80)
    private String navigationModuleCode;

    @Column(name = "navigation_module_name", nullable = false, length = 150)
    private String navigationModuleName;

    @Column(name = "physical_module_code", length = 80)
    private String physicalModuleCode;

    @Column(length = 80)
    private String icon;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active;
}
