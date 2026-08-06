package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.config.AccessControlProperties;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApiRegistryService;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserPrivilegeApiAccessFilter extends OncePerRequestFilter {

    private final ClientApiRegistryService clientApiRegistryService;
    private final PrivilegeService privilegeService;
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
            AccessControlError error = AccessControlError.USER_PRIVILEGE_NOT_ALLOWED;
            responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

}
