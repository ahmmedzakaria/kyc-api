package com.nexacore.systemmodule.layout.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysLayoutRoutePolicyPrivilegeId implements Serializable {
    @Column(name = "route_policy_id")
    private Long routePolicyId;

    @Column(name = "privilege_id")
    private Long privilegeId;
}
