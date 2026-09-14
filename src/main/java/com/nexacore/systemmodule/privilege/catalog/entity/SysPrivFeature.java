package com.nexacore.systemmodule.privilege.catalog.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import com.nexacore.systemmodule.layout.entity.SysLayoutNavigationCategory;
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
        name = "sys_priv_features",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sys_features_submodule_type_code",
                columnNames = {"submodule_id", "navigation_category_id", "feature_code"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysPrivFeature extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submodule_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPrivSubmodule submodule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navigation_category_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLayoutNavigationCategory navigationCategory;

    @Column(name = "feature_code", nullable = false, length = 3)
    private String featureCode;

    @Column(name = "feature_name", nullable = false)
    private String featureName;

    @Column(nullable = false)
    private boolean active;

    public String getFeatureTypeCode() {
        return navigationCategory == null ? null : navigationCategory.getFeatureTypeCode();
    }

    public String getFeatureTypeName() {
        return navigationCategory == null ? null : navigationCategory.getCategoryName();
    }
}
