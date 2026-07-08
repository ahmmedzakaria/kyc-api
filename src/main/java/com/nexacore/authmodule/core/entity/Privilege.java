package com.nexacore.authmodule.core.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Privilege extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String privilegeCode;

    @Column(nullable = false, length = 2)
    private String moduleCode;

    @Column(nullable = false)
    private String moduleName;

    @Column(nullable = false, length = 2)
    private String submoduleCode;

    @Column(nullable = false)
    private String submoduleName;

    @Column(nullable = false, length = 2)
    private String featureTypeCode;

    @Column(nullable = false)
    private String featureTypeName;

    @Column(nullable = false, length = 3)
    private String featureCode;

    @Column(nullable = false)
    private String featureName;

    @Column(nullable = false, length = 2)
    private String actionCode;

    @Column(nullable = false)
    private String actionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_menu_id")
    @EqualsAndHashCode.Exclude
    private SubMenu subMenu;

    @Column(nullable = false)
    private boolean active;
}
