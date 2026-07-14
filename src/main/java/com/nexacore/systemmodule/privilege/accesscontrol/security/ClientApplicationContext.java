package com.nexacore.systemmodule.privilege.accesscontrol.security;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;
import lombok.Builder;

@Builder
public record ClientApplicationContext(
        String traceId,
        SysClientApplication clientApplication,
        SysApiRegistry apiRegistry,
        String decision,
        String denyReason
) {
}
