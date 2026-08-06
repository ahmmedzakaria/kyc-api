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
public class SysLayoutUiPolicyPrivilegeId implements Serializable {
    @Column(name = "ui_policy_id")
    private Long uiPolicyId;

    @Column(name = "privilege_id")
    private Long privilegeId;
}
