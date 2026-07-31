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
@Table(name = "sys_layout_profile_branding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutProfileBranding extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_id", nullable = false)
    private SysLayoutProfile layoutProfile;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "short_name", length = 80)
    private String shortName;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_dark_url")
    private String logoDarkUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "support_url")
    private String supportUrl;

    @Column(nullable = false)
    private boolean active;
}
