package com.nexacore.systemmodule.privilege.assignment.entity;

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
public class RolePrivilegeId implements Serializable {

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "privilege_id", nullable = false)
    private Long privilegeId;
}
