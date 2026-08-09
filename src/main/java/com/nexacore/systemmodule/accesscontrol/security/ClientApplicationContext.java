package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import lombok.Builder;

@Builder
public record ClientApplicationContext(
        String traceId,
        SysAccClientApplication clientApplication,
        SysAccApiRegistry apiRegistry,
        String requiredPrivilegeCode,
        String clientDecision,
        String clientDenyReason,
        String userDecision,
        String userDenyReason,
        Long userId,
        java.util.Set<UserScopeAssignment> scopeAssignments
) {
    public String decision() {
        if ("DENIED".equals(userDecision) || "DENIED".equals(clientDecision)) return "DENIED";
        if (userDecision != null) return userDecision;
        return clientDecision;
    }

    public String denyReason() {
        return userDenyReason != null ? userDenyReason : clientDenyReason;
    }
}
