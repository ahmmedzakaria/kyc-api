package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.layout.enums.LayoutDensity;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.enums.NavigationMode;
import com.nexacore.systemmodule.layout.enums.ThemeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutProfile extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_code", nullable = false, unique = true, length = 80)
    private String profileCode;

    @Column(name = "profile_name", nullable = false, length = 150)
    private String profileName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "layout_type", nullable = false, length = 40, columnDefinition = "varchar(40)")
    private LayoutType layoutType;

    @Enumerated(EnumType.STRING)
    @Column(name = "navigation_mode", nullable = false, length = 40, columnDefinition = "varchar(40)")
    private NavigationMode navigationMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "theme_mode", nullable = false, length = 40, columnDefinition = "varchar(40)")
    private ThemeMode themeMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, columnDefinition = "varchar(40)")
    private LayoutDensity density;

    @Column(name = "topbar_enabled", nullable = false)
    private boolean topbarEnabled;

    @Column(name = "sidebar_enabled", nullable = false)
    private boolean sidebarEnabled;

    @Column(name = "sidebar_collapsed", nullable = false)
    private boolean sidebarCollapsed;

    @Column(name = "footer_enabled", nullable = false)
    private boolean footerEnabled;

    @Column(name = "breadcrumb_enabled", nullable = false)
    private boolean breadcrumbEnabled;

    @Column(name = "command_bar_enabled", nullable = false)
    private boolean commandBarEnabled;

    @Column(name = "rtl_enabled", nullable = false)
    private boolean rtlEnabled;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    void defaults() {
        super.prePersist();
        layoutType = layoutType == null ? LayoutType.RAIL : layoutType;
        navigationMode = navigationMode == null ? NavigationMode.MODULE_GROUP_MEGA_PANEL : navigationMode;
        themeMode = themeMode == null ? ThemeMode.LIGHT : themeMode;
        density = density == null ? LayoutDensity.COMFORTABLE : density;
    }
}
