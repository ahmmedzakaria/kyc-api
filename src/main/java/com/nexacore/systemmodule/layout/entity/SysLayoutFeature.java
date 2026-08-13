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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_features")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutFeature extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_group_id", nullable = false)
    private SysLayoutFeatureGroup featureGroup;

    @Column(name = "feature_code", nullable = false, length = 80)
    private String featureCode;

    @Column(name = "t_code", length = 30)
    private String tCode;

    @Column(name = "feature_name", nullable = false, length = 150)
    private String featureName;

    @Column(nullable = false)
    private String route;

    @Column(length = 80)
    private String icon;

    @Column(name = "physical_module_code", length = 80)
    private String physicalModuleCode;

    @Column(name = "physical_submodule_code", length = 80)
    private String physicalSubmoduleCode;

    @Column(name = "physical_feature_type_code", length = 80)
    private String physicalFeatureTypeCode;

    @Column(name = "physical_feature_code", length = 80)
    private String physicalFeatureCode;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(nullable = false)
    private boolean active;
}
