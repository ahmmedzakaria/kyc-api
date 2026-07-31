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
@Table(name = "sys_layout_theme_chrome_overrides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutThemeChromeOverrides extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_theme_id", nullable = false)
    private SysLayoutProfileTheme layoutProfileTheme;

    @Column(name = "accent_soft", length = 40)
    private String accentSoft;

    @Column(name = "background", length = 40)
    private String bg;

    @Column(name = "border", length = 40)
    private String border;

    @Column(name = "border_strong", length = 40)
    private String borderStrong;

    @Column(name = "hover_background", length = 40)
    private String hoverBg;

    @Column(name = "active_background", length = 40)
    private String activeBg;

    @Column(name = "search_background", length = 40)
    private String searchBg;

    @Column(name = "search_border", length = 40)
    private String searchBorder;

    @Column(name = "search_text", length = 40)
    private String searchText;

    @Column(name = "search_placeholder", length = 40)
    private String searchPlaceholder;

    @Column(nullable = false)
    private boolean active;
}
