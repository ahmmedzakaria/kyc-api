package com.nexacore.systemmodule.privilege.catalog.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_priv_feature_types")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysPrivFeatureType extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;

    @Column(name = "feature_type_code", nullable = false, length = 2)
    private String featureTypeCode;

    @Column(name = "feature_type_name", nullable = false)
    private String featureTypeName;

    private String description;

    @Column(nullable = false)
    private boolean active;

    private Long createdBy;
    private Long updatedBy;
}
