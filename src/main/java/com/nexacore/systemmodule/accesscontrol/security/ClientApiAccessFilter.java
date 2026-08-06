package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.config.EnforcementMode;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
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
        String path = request.getServletPath();
        return !accessControlProperties.isDecisionEvaluationEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || publicRoutePolicy.isPublic(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Optional<SysPrivApiRegistry> api = clientApiRegistryService.resolve(request);
        if (api.isEmpty()) {
            if (accessControlProperties.isRegistryCoverageEnabled()
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

        SysPrivClientApplication application = ClientApplicationContextHolder.get()
                .map(ClientApplicationContext::clientApplication)
                .orElse(null);
        ClientAccessDecisionDto decision = clientAccessDecisionService.decide(application, api.get());
        updateContext(decision);

        if (!decision.allowed()) {
            if ("CLIENT_REQUIRED".equals(decision.denyReason()) && !requiresClient(request)) {
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
        ClientApplicationContext existing = ClientApplicationContextHolder.get().orElse(null);
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(existing == null ? null : existing.traceId())
                .clientApplication(decision.clientApplication())
                .apiRegistry(decision.apiRegistry())
                .decision(decision.allowed() ? "ALLOWED" : "DENIED")
                .denyReason(decision.denyReason())
                .build());
    }

    private void logWouldDeny(ClientAccessDecisionDto decision) {
        ClientApplicationContext context = ClientApplicationContextHolder.get().orElse(null);
        SysPrivApiRegistry api = decision.apiRegistry();
        SysPrivClientApplication client = decision.clientApplication();
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
