package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/authenticate",
            "/api/v1/auth/config",
            "/api/v1/auth/application-context/public",
            "/api/v1/auth/sso/authenticate",
            "/api/v1/auth/login-status",
            "/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ClientCredentialService clientCredentialService;
    private final ApiResponseJsonWriter responseWriter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || matchesPublicPath(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        response.setHeader(TRACE_ID_HEADER, traceId);
        SysPrivClientApplication application = null;

        try {
            String clientCode = request.getHeader(CLIENT_CODE_HEADER);
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (hasText(clientCode) || hasText(apiKey)) {
                Optional<SysPrivClientApplication> resolvedClient = clientCredentialService.validateApiKey(clientCode, apiKey);
                if (resolvedClient.isEmpty()) {
                    setContext(traceId, null, "DENIED", "INVALID_CLIENT_CREDENTIALS");
                    AccessControlError error = AccessControlError.INVALID_CLIENT_CREDENTIALS;
                    responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
                    return;
                }
                application = resolvedClient.get();
            }

            setContext(traceId, application, null, null);
            filterChain.doFilter(request, response);
        } finally {
            ClientApplicationContextHolder.clear();
        }
    }

    private void setContext(String traceId, SysPrivClientApplication application, String decision, String denyReason) {
        ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                .traceId(traceId)
                .clientApplication(application)
                .decision(decision)
                .denyReason(denyReason)
                .build());
    }

    private String resolveTraceId(HttpServletRequest request) {
        String existingTraceId = request.getHeader(TRACE_ID_HEADER);
        return hasText(existingTraceId) ? existingTraceId.trim() : UUID.randomUUID().toString();
    }

    private boolean matchesPublicPath(String path) {
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
