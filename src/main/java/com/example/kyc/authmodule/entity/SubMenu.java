package com.example.kyc.authmodule.entity;

import com.example.kyc.commonmodule.dto.ActionInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sub_menus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubMenu extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column
    private String icon;

    @Column(nullable = false, length = 2)
    private String moduleCode;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false, length = 2)
    private String featureTypeCode;

    @Column(nullable = false)
    private String featureTypeName;

    @Column(nullable = false, length = 3)
    private String featureCode;

    @Column(nullable = false)
    private String featureName;

    @Column(nullable = false)
    private boolean active;

    @Column
    private Long createdBy;

    @Column
    private Long updatedBy;
}
