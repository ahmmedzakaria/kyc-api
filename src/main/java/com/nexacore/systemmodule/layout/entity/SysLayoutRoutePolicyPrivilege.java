package com.nexacore.systemmodule.layout.entity;

import com.nexacore.systemmodule.privilege.catalog.entity.SysPrivPrivilege;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_layout_route_policy_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class SysLayoutRoutePolicyPrivilege extends LayoutAuditInfo {
    @EmbeddedId
    private SysLayoutRoutePolicyPrivilegeId id;

    @MapsId("routePolicyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_policy_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLayoutRoutePolicy routePolicy;

    @MapsId("privilegeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "privilege_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysPrivPrivilege privilege;

    @Column(nullable = false)
    private boolean active;
}
