package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
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
