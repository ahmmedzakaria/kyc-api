package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
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
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AuthenticatedRequestContextFilter extends OncePerRequestFilter {
    private final AuthModuleGateway authModuleGateway;
    private final PrivilegeService privilegeService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                AuthUserAccessDto user = authModuleGateway.getUserAccess(username);
                ClientApplicationContext accessContext = ClientApplicationContextHolder.get().orElse(null);
                SysPrivClientApplication client = accessContext == null ? null : accessContext.clientApplication();
                Set<String> privileges = privilegeService.getUserPrivilegeCodes(username);

                AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                        user.userId(), username,
                        client == null ? null : client.getId(),
                        client == null ? null : client.getClientCode(),
                        user.tenantId(), user.businessId(), user.branchId(),
                        accessContext == null ? null : accessContext.traceId(),
                        privileges
                ));
            }
            filterChain.doFilter(request, response);
        } finally {
            AuthenticatedRequestContextHolder.clear();
        }
    }
}
