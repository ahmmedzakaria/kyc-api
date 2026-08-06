package com.nexacore.systemmodule.privilege.catalog.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_priv_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysPrivPrivilege extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String privilegeCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPrivFeature feature;

    @Transient
    private String moduleCode;

    @Transient
    private String moduleName;

    @Transient
    private String submoduleCode;

    @Transient
    private String submoduleName;

    @Transient
    private String featureTypeCode;

    @Transient
    private String featureTypeName;

    @Transient
    private String featureCode;

    @Transient
    private String featureName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPrivAction action;

    @Transient
    private String actionCode;

    @Transient
    private String actionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_menu_id")
    @EqualsAndHashCode.Exclude
    private SysPrivSubMenu subMenu;

    @Column(nullable = false)
    private boolean active;

    public String getModuleCode() {
        return feature != null && feature.getSubmodule() != null && feature.getSubmodule().getModule() != null
                ? feature.getSubmodule().getModule().getCode()
                : moduleCode;
    }

    public String getModuleName() {
        return feature != null && feature.getSubmodule() != null && feature.getSubmodule().getModule() != null
                ? feature.getSubmodule().getModule().getName()
                : moduleName;
    }

    public String getSubmoduleCode() {
        return feature != null && feature.getSubmodule() != null
                ? feature.getSubmodule().getCode()
                : submoduleCode;
    }

    public String getSubmoduleName() {
        return feature != null && feature.getSubmodule() != null
                ? feature.getSubmodule().getName()
                : submoduleName;
    }

    public String getFeatureTypeCode() {
        return feature != null ? feature.getFeatureTypeCode() : featureTypeCode;
    }

    public String getFeatureTypeName() {
        return feature != null ? feature.getFeatureTypeName() : featureTypeName;
    }

    public String getFeatureCode() {
        return feature != null ? feature.getFeatureCode() : featureCode;
    }

    public String getFeatureName() {
        return feature != null ? feature.getFeatureName() : featureName;
    }

    public String getActionCode() {
        return action != null ? action.getActionCode() : actionCode;
    }

    public String getActionName() {
        return action != null ? action.getActionName() : actionName;
    }
}
