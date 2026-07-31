package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.layout.enums.FontSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sys_layout_profile_fonts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutProfileFonts extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_id", nullable = false)
    private SysLayoutProfile layoutProfile;

    @Column(name = "body_family", nullable = false, length = 120)
    private String bodyFamily;

    @Column(name = "heading_family", length = 120)
    private String headingFamily;

    @Column(name = "mono_family", length = 120)
    private String monoFamily;

    @Enumerated(EnumType.STRING)
    @Column(name = "font_source", nullable = false, length = 30)
    private FontSource fontSource;

    @Column(name = "font_url")
    private String fontUrl;

    @Column(name = "fallback_stack", nullable = false)
    private String fallbackStack;

    @Column(nullable = false)
    private boolean active;
}
