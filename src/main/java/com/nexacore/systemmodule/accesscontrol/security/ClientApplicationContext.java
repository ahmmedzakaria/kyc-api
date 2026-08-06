package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import lombok.Builder;

@Builder
public record ClientApplicationContext(
        String traceId,
        SysPrivClientApplication clientApplication,
        SysPrivApiRegistry apiRegistry,
        String requiredPrivilegeCode,
        String clientDecision,
        String clientDenyReason,
        String userDecision,
        String userDenyReason
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
