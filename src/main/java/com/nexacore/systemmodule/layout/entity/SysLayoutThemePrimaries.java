package com.nexacore.systemmodule.layout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_theme_primaries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutThemePrimaries extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_theme_id", nullable = false)
    private SysLayoutProfileTheme layoutProfileTheme;

    @Column(name = "text_color", nullable = false, length = 40)
    private String textColor;

    @Column(name = "paper_color", nullable = false, length = 40)
    private String paperColor;

    @Column(name = "card_color", nullable = false, length = 40)
    private String cardColor;

    @Column(name = "accent_color", nullable = false, length = 40)
    private String accentColor;

    @Column(name = "amber_color", nullable = false, length = 40)
    private String amberColor;

    @Column(name = "red_color", nullable = false, length = 40)
    private String redColor;

    @Column(name = "success_color", nullable = false, length = 40)
    private String successColor;

    @Column(name = "info_color", nullable = false, length = 40)
    private String infoColor;

    @Column(nullable = false)
    private boolean active;
}
