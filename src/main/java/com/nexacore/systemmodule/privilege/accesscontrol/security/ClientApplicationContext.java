package com.nexacore.systemmodule.privilege.accesscontrol.security;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;
import lombok.Builder;

@Builder
public record ClientApplicationContext(
        String traceId,
        SysPrivClientApplication clientApplication,
        SysPrivApiRegistry apiRegistry,
        String decision,
        String denyReason
) {
}
