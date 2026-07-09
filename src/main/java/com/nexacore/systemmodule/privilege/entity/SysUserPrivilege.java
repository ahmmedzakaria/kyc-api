package com.nexacore.systemmodule.privilege.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sys_user_privileges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysUserPrivilege {

    @EmbeddedId
    private UserPrivilegeId id;
}
