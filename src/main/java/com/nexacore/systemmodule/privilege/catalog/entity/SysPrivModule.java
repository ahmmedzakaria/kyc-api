package com.nexacore.systemmodule.privilege.catalog.entity;

import com.nexacore.systemmodule.layout.entity.LayoutAuditInfo;
import com.nexacore.systemmodule.layout.entity.SysLayoutModuleGroup;
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
@Table(name = "sys_priv_modules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysPrivModule extends LayoutAuditInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "module_group_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLayoutModuleGroup moduleGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_module_id")
    @EqualsAndHashCode.Exclude
    private SysPhysicalModule physicalModule;

    @Column(name = "navigation_module_code", nullable = false, unique = true, length = 80)
    private String navigationModuleCode;
    @Column(name = "navigation_module_name", nullable = false, length = 150)
    private String name;
    @Column(name = "module_priv_code", unique = true, length = 2)
    private String modulePrivCode;
    @Column(name = "physical_module_code", length = 80)
    private String physicalModuleCode;
    @Column(length = 80) private String icon;
    @Column(columnDefinition = "text") private String description;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    @Column(name = "navigation_enabled", nullable = false) private boolean navigationEnabled;
    @Column(nullable = false) private boolean active;

    public String getNavigationModuleName() { return name; }
    public void setNavigationModuleName(String navigationModuleName) { this.name = navigationModuleName; }
}
