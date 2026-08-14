package com.nexacore.systemmodule.tenant.security;

import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.TenantDomainResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    private final TenantDomainResolver tenantDomainResolver;
    private final RequestTenantHostnameResolver requestHostnameResolver;
    private final ApiResponseJsonWriter responseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String hostname = requestHostnameResolver.resolve(request);
            var tenant = tenantDomainResolver.resolveVerified(hostname).orElse(null);
            if (tenant == null) {
                deny(response, 403, "TENANT_DOMAIN_NOT_RECOGNIZED", "The request domain is not assigned to a verified tenant");
                return;
            }
            if (tenant.status() != TenantStatus.ACTIVE) {
                deny(response, 403, "TENANT_NOT_ACTIVE", "The resolved tenant is not active");
                return;
            }
            ResolvedTenantContext context = new ResolvedTenantContext(tenant.id(), tenant.tenantCode(), hostname);
            ResolvedTenantContextHolder.set(context);
            request.setAttribute(ResolvedTenantContext.class.getName(), context);
            response.setHeader(TENANT_ID_HEADER, Long.toString(context.tenantId()));
            response.setHeader(TENANT_CODE_HEADER, context.tenantCode());
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException exception) {
            deny(response, 400, "TENANT_HOST_INVALID", "The request hostname is invalid");
        } finally {
            ResolvedTenantContextHolder.clear();
        }
    }

    private void deny(HttpServletResponse response, int status, String code, String message) throws IOException {
        responseWriter.writeError(response, status, code, message);
    }
}
