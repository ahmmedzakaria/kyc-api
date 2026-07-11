package com.nexacore.systemmodule.clientaccess.security;

import com.nexacore.systemmodule.clientaccess.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientApiRegistryService;
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

@Component
@RequiredArgsConstructor
public class ClientApiAccessFilter extends OncePerRequestFilter {

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/authenticate",
            "/auth/config",
            "/auth/application-context/public",
            "/auth/sso/authenticate",
            "/auth/login-status",
            "/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ClientApiRegistryService clientApiRegistryService;
    private final ClientAccessDecisionService clientAccessDecisionService;
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
        Optional<SysApiRegistry> api = clientApiRegistryService.resolve(request);
        if (api.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        SysClientApplication application = ClientApplicationContextHolder.get()
                .map(ClientApplicationContext::clientApplication)
                .orElse(null);
        ClientAccessDecisionDto decision = clientAccessDecisionService.decide(application, api.get());
        updateContext(decision);

        if (!decision.allowed()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, decision.denyReason());
            return;
        }

        filterChain.doFilter(request, response);
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

    private boolean matchesPublicPath(String path) {
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
