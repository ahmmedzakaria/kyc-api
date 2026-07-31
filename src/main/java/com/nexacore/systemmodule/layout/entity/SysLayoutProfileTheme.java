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
@Table(name = "sys_layout_profile_themes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutProfileTheme extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_id", nullable = false)
    private SysLayoutProfile layoutProfile;

    @Column(name = "theme_id", nullable = false, length = 80)
    private String themeId;

    @Column(name = "theme_label", nullable = false, length = 150)
    private String themeLabel;

    @Column(nullable = false, length = 20)
    private String base;

    @Column(nullable = false, length = 40)
    private String swatch;

    @Column(nullable = false)
    private boolean active;
}
