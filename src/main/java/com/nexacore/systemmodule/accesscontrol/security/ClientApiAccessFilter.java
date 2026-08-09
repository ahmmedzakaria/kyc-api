package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClientApiAccessFilter extends OncePerRequestFilter {

    private final ClientApiRegistryService clientApiRegistryService;
    private final ClientAccessDecisionService clientAccessDecisionService;
    private final ApiResponseJsonWriter responseWriter;
    private final AccessControlProperties accessControlProperties;
    private final PublicRoutePolicy publicRoutePolicy;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return !accessControlProperties.isDecisionEvaluationEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || publicRoutePolicy.isPublic(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<SysAccApiRegistry> api;
        try {
            api = clientApiRegistryService.resolve(request);
        } catch (ApiRouteAmbiguityException exception) {
            AccessControlError error = AccessControlError.API_REGISTRY_AMBIGUOUS;
            updateDeniedContext(error.name());
            responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
            return;
        }
        if (api.isEmpty()) {
            updateUnresolvedContext();
            if (isApplicationApi(request)
                    && accessControlProperties.getEnforcementMode() == EnforcementMode.ENFORCE) {
                AccessControlError error = AccessControlError.API_NOT_REGISTERED;
                responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
                return;
            }
            if (accessControlProperties.isRegistryCoverageEnabled()) {
                log.warn("Access-control registry coverage gap: method={} path={} mode={}",
                        request.getMethod(), request.getRequestURI(), accessControlProperties.getEnforcementMode());
            }
            filterChain.doFilter(request, response);
            return;
        }

        SysAccClientApplication application = ClientApplicationContextHolder.get()
                .map(ClientApplicationContext::clientApplication)
                .orElse(null);
        ClientAccessDecisionDto decision = clientAccessDecisionService.decide(application, api.get());
        updateContext(decision);

        if (!decision.allowed()) {
            if ("CLIENT_REQUIRED".equals(decision.denyReason()) && !requiresClient(request)) {
                updateClientDecision(decision, "NOT_REQUIRED", null);
                filterChain.doFilter(request, response);
                return;
            }
            if (accessControlProperties.getEnforcementMode() == EnforcementMode.REPORT) {
                logWouldDeny(decision);
                filterChain.doFilter(request, response);
                return;
            }
            AccessControlError error = AccessControlError.fromClientDecision(decision.denyReason());
            responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresClient(HttpServletRequest request) {
        boolean browserRequest = request.getHeader("Origin") != null
                || request.getHeader("Sec-Fetch-Site") != null;
        return browserRequest
                ? accessControlProperties.isRequireClientForBrowser()
                : accessControlProperties.isRequireClientForConfidential();
    }

    private void updateContext(ClientAccessDecisionDto decision) {
        updateClientDecision(decision, decision.allowed() ? "ALLOWED" : "DENIED", decision.denyReason());
    }

    private void updateClientDecision(ClientAccessDecisionDto decision, String status, String denyReason) {
        ClientApplicationContext existing = ClientApplicationContextHolder.get().orElse(null);
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(existing == null ? null : existing.traceId())
                .clientApplication(decision.clientApplication())
                .apiRegistry(decision.apiRegistry())
                .requiredPrivilegeCode(decision.apiRegistry() == null
                        ? null : decision.apiRegistry().getRequiredPrivilegeCode())
                .clientDecision(status)
                .clientDenyReason(denyReason)
                .userDecision(existing == null ? null : existing.userDecision())
                .userDenyReason(existing == null ? null : existing.userDenyReason())
                .userId(existing == null ? null : existing.userId())
                .scopeAssignments(existing == null ? java.util.Set.of() : existing.scopeAssignments())
                .build());
    }

    private void updateUnresolvedContext() {
        ClientApplicationContext existing = ClientApplicationContextHolder.get().orElse(null);
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(existing == null ? null : existing.traceId())
                .clientApplication(existing == null ? null : existing.clientApplication())
                .clientDecision("DENIED")
                .clientDenyReason(AccessControlError.API_NOT_REGISTERED.name())
                .userId(existing == null ? null : existing.userId())
                .scopeAssignments(existing == null ? java.util.Set.of() : existing.scopeAssignments())
                .build());
    }

    private void updateDeniedContext(String denialCode) {
        ClientApplicationContext existing = ClientApplicationContextHolder.get().orElse(null);
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(existing == null ? null : existing.traceId())
                .clientApplication(existing == null ? null : existing.clientApplication())
                .clientDecision("DENIED").clientDenyReason(denialCode)
                .userId(existing == null ? null : existing.userId())
                .scopeAssignments(existing == null ? java.util.Set.of() : existing.scopeAssignments())
                .build());
    }

    private boolean isApplicationApi(HttpServletRequest request) {
        String path = requestPath(request);
        return path != null && (path.equals("/api") || path.startsWith("/api/"));
    }

    private String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
    }

    private void logWouldDeny(ClientAccessDecisionDto decision) {
        ClientApplicationContext context = ClientApplicationContextHolder.get().orElse(null);
        SysAccApiRegistry api = decision.apiRegistry();
        SysAccClientApplication client = decision.clientApplication();
        log.warn("access-control would-deny traceId={} apiCode={} clientCode={} username={} "
                        + "requiredPrivilegeCode={} denialReason={}",
                context == null ? null : context.traceId(),
                api == null ? null : api.getApiCode(),
                client == null ? null : client.getClientCode(),
                null,
                api == null ? null : api.getRequiredPrivilegeCode(),
                decision.denyReason());
    }

}
