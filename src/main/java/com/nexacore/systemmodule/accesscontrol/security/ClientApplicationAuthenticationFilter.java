package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientApplicationAuthenticationFilter extends OncePerRequestFilter {

    public static final String CLIENT_CODE_HEADER = "X-Client-Code";
    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final ClientCredentialService clientCredentialService;
    private final ClientOriginPolicy clientOriginPolicy;
    private final ClientIpPolicy clientIpPolicy;
    private final ClientRateLimiter clientRateLimiter;
    private final PublicRoutePolicy publicRoutePolicy;
    private final ApiResponseJsonWriter responseWriter;
    private final AccessControlProperties accessControlProperties;
    private final AuthorizationEventEmitter authorizationEventEmitter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = requestPath(request);
        return request.getDispatcherType() == DispatcherType.ERROR
                || !accessControlProperties.isDecisionEvaluationEnabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || publicRoutePolicy.isPublic(path);
    }

    private String requestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath == null || servletPath.isBlank() ? request.getRequestURI() : servletPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        System.out.println(traceId + " ClientApplicationAuthenticationFilter");
        long startedAt = System.nanoTime();
        response.setHeader(TRACE_ID_HEADER, traceId);
        SysAccClientApplication application = null;

        try {
            String clientCode = request.getHeader(CLIENT_CODE_HEADER);
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (hasText(apiKey) && !hasText(clientCode)) {
                deny(response, traceId, AccessControlError.INVALID_CLIENT_CREDENTIALS);
                return;
            }
            if (hasText(clientCode)) {
                Optional<SysAccClientApplication> resolvedClient = clientCredentialService.resolveActiveClient(clientCode);
                if (resolvedClient.isEmpty()) {
                    deny(response, traceId, AccessControlError.INVALID_CLIENT_CREDENTIALS);
                    return;
                }
                SysAccClientApplication candidate = resolvedClient.get();
                if (candidate.getClientType() == null || candidate.getClientType().isConfidential()) {
                    resolvedClient = clientCredentialService.validateApiKey(clientCode, apiKey);
                    if (resolvedClient.isEmpty()) {
                        deny(response, traceId, AccessControlError.INVALID_CLIENT_CREDENTIALS);
                        return;
                    }
                    candidate = resolvedClient.get();
                }
                if (!clientOriginPolicy.isAllowed(candidate, request.getHeader("Origin"))) {
                    deny(response, traceId, candidate, AccessControlError.CLIENT_ORIGIN_NOT_ALLOWED);
                    return;
                }
                if (!clientIpPolicy.isAllowed(candidate, request)) {
                    deny(response, traceId, candidate, AccessControlError.CLIENT_IP_NOT_ALLOWED);
                    return;
                }
                try {
                    ClientRateLimitDecision rateLimit = clientRateLimiter.check(candidate, request);
                    applyRateLimitHeaders(response, rateLimit);
                    if (!rateLimit.allowed()) {
                        deny(response, traceId, candidate, AccessControlError.RATE_LIMIT_EXCEEDED);
                        return;
                    }
                } catch (RateLimitBackendUnavailableException ex) {
                    deny(response, traceId, candidate, AccessControlError.RATE_LIMIT_UNAVAILABLE);
                    return;
                }
                application = candidate;
            }

            setContext(traceId, application, null, null);
            filterChain.doFilter(request, response);
        } finally {
            authorizationEventEmitter.emit(request.getMethod(), ClientApplicationContextHolder.get().orElse(null),
                    System.nanoTime() - startedAt);
            ClientApplicationContextHolder.clear();
        }
    }

    private void deny(HttpServletResponse response,
                      String traceId,
                      AccessControlError error) throws IOException {
        deny(response, traceId, null, error);
    }

    private void deny(HttpServletResponse response,
                      String traceId,
                      SysAccClientApplication application,
                      AccessControlError error) throws IOException {
        setContext(traceId, application, "DENIED", error.name());
        responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
    }

    private void setContext(String traceId, SysAccClientApplication application, String decision, String denyReason) {
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(traceId)
                .clientApplication(application)
                .clientDecision(decision)
                .clientDenyReason(denyReason)
                .scopeAssignments(java.util.Set.of())
                .build());
    }

    private void applyRateLimitHeaders(HttpServletResponse response, ClientRateLimitDecision decision) {
        if (decision.limit() <= 0) return;
        response.setHeader("RateLimit-Limit", Long.toString(decision.limit()));
        response.setHeader("RateLimit-Remaining", Long.toString(decision.remaining()));
        response.setHeader("RateLimit-Reset", Long.toString(decision.retryAfterSeconds()));
        if (!decision.allowed()) response.setHeader("Retry-After", Long.toString(decision.retryAfterSeconds()));
    }

    private String resolveTraceId(HttpServletRequest request) {
        String existingTraceId = request.getHeader(TRACE_ID_HEADER);
        return hasText(existingTraceId) ? existingTraceId.trim() : UUID.randomUUID().toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
