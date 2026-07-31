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
@Table(name = "sys_layout_profile_sizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutProfileSizes extends LayoutAuditInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_profile_id", nullable = false)
    private SysLayoutProfile layoutProfile;

    @Column(name = "space_unit", nullable = false)
    private Double spaceUnit;

    @Column(name = "radius_base", nullable = false)
    private Double radiusBase;

    @Column(name = "font_size_base", nullable = false)
    private Double fontSizeBase;

    @Column(name = "header_height", nullable = false)
    private Double headerHeight;

    @Column(name = "status_bar_height", nullable = false)
    private Double statusBarHeight;

    @Column(name = "rail_width_collapsed", nullable = false)
    private Double railWidthCollapsed;

    @Column(name = "rail_width_expanded", nullable = false)
    private Double railWidthExpanded;

    @Column(nullable = false)
    private boolean active;
}
