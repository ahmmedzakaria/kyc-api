package com.nexacore.systemmodule.privilege.accesscontrol.security;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysApiRegistry;
import com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPrivilegeApiAccessFilter extends OncePerRequestFilter {

    private static final String[] PUBLIC_PATHS = {
            "/auth/login",
            "/auth/authenticate",
            "/auth/config",
            "/auth/sso/authenticate",
            "/auth/login-status",
            "/oauth2/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final ClientApiRegistryService clientApiRegistryService;
    private final PrivilegeService privilegeService;
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
        if (api.isEmpty()
                || api.get().isPublicApi()
                || api.get().getRequiredPrivilegeCode() == null
                || api.get().getRequiredPrivilegeCode().isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean allowed = privilegeService.getUserPrivilegeCodes(authentication.getName())
                .contains(api.get().getRequiredPrivilegeCode());
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "USER_PRIVILEGE_NOT_ALLOWED");
            return;
        }

        filterChain.doFilter(request, response);
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
