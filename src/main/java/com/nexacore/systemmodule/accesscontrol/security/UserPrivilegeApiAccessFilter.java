package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPrivilegeApiAccessFilter extends OncePerRequestFilter {

    private final ApiResponseJsonWriter responseWriter;
    private final AccessControlProperties accessControlProperties;
    private final PublicRoutePolicy publicRoutePolicy;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String path = servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
        return !accessControlProperties.isDecisionEvaluationEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || publicRoutePolicy.isPublic(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        System.out.println("UserPrivilegeApiAccessFilter -> doFilterInternal");
        ClientApplicationContext context = ClientApplicationContextHolder.get().orElse(null);
        SysPrivApiRegistry api = context == null ? null : context.apiRegistry();
        if (api == null
                || api.isPublicApi()
                || context.requiredPrivilegeCode() == null
                || context.requiredPrivilegeCode().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        AuthenticatedRequestContext authenticatedContext = AuthenticatedRequestContextHolder.get().orElse(null);
        boolean allowed = authenticatedContext != null
                && authenticatedContext.effectivePrivilegeCodes().contains(context.requiredPrivilegeCode());
        updateUserDecision(context, allowed ? "ALLOWED" : "DENIED",
                allowed ? null : AccessControlError.USER_PRIVILEGE_NOT_ALLOWED.name());
        if (!allowed) {
            if (accessControlProperties.getEnforcementMode()
                    == com.nexacore.systemmodule.accesscontrol.config.EnforcementMode.REPORT) {
                log.warn("access-control would-deny traceId={} apiCode={} clientCode={} username={} "
                                + "requiredPrivilegeCode={} denialReason={}",
                        context == null ? null : context.traceId(),
                        api.getApiCode(),
                        context == null || context.clientApplication() == null
                                ? null : context.clientApplication().getClientCode(),
                        authentication.getName(),
                        context.requiredPrivilegeCode(),
                        AccessControlError.USER_PRIVILEGE_NOT_ALLOWED.name());
                filterChain.doFilter(request, response);
                return;
            }
            AccessControlError error = AccessControlError.USER_PRIVILEGE_NOT_ALLOWED;
            responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void updateUserDecision(ClientApplicationContext context, String status, String denyReason) {
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(context.traceId())
                .clientApplication(context.clientApplication())
                .apiRegistry(context.apiRegistry())
                .requiredPrivilegeCode(context.requiredPrivilegeCode())
                .clientDecision(context.clientDecision())
                .clientDenyReason(context.clientDenyReason())
                .userDecision(status)
                .userDenyReason(denyReason)
                .userId(context.userId())
                .scopeAssignments(context.scopeAssignments())
                .build());
    }

}
