package com.nexacore.systemmodule.tenant.security;

import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Resolves the tenant-facing hostname selected by the trusted routing layer. */
@Component
@RequiredArgsConstructor
public class RequestTenantHostnameResolver {
    private final HostnameNormalizer hostnameNormalizer;

    public String resolve(HttpServletRequest request) {
        // Origin and X-Forwarded-Host are caller-controlled at this layer. The edge proxy
        // preserves the routed Host, which the servlet container exposes as serverName.
        return hostnameNormalizer.normalize(request.getServerName());
    }
}
