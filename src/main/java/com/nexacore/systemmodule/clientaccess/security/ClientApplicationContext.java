package com.nexacore.systemmodule.clientaccess.security;

import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
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
