package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
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
import java.util.Set;
import com.nexacore.authmodule.security.service.TenantAccountUserDetails;
import com.nexacore.systemmodule.tenant.security.RequestTenantHostnameResolver;

@Component
@RequiredArgsConstructor
public class AuthenticatedRequestContextFilter extends OncePerRequestFilter {
    private final AuthModuleGateway authModuleGateway;
    private final PrivilegeService privilegeService;
    private final EffectiveTenantAccessResolver effectiveTenantAccessResolver;
    private final ApiResponseJsonWriter responseWriter;
    private final RequestTenantHostnameResolver requestHostnameResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                if (!(authentication.getPrincipal() instanceof TenantAccountUserDetails principal)) {
                    throw new IllegalStateException("Tenant-bound authenticated principal is required");
                }
                AuthUserAccessDto user = authModuleGateway.getUserAccess(principal.accountId(), principal.tenantId());
                ClientApplicationContext accessContext = ClientApplicationContextHolder.get().orElse(null);
                SysAccClientApplication client = accessContext == null ? null : accessContext.clientApplication();
                Set<String> privileges = privilegeService.getUserPrivilegeCodes(username);

                Set<UserScopeAssignment> scopes = user.scopeAssignments() == null ? Set.of() : user.scopeAssignments().stream()
                        .map(scope -> new UserScopeAssignment(scope.tenantId(), scope.businessId(), scope.branchId()))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                EffectiveTenantAccessContext effectiveTenant = effectiveTenantAccessResolver.resolve(
                        requestHostnameResolver.resolve(request), principal.accountId(), principal.tenantId(), client, scopes);
                EffectiveTenantAccessContextHolder.set(effectiveTenant);

                AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                        user.userId(), username,
                        client == null ? null : client.getId(),
                        client == null ? null : client.getClientCode(),
                        effectiveTenant.effectiveScopes(),
                        accessContext == null ? null : accessContext.traceId(),
                        privileges
                ));
                ClientApplicationContext current = ClientApplicationContextHolder.get().orElse(null);
                if (current != null) {
                    ClientApplicationContextHolder.set(ClientApplicationContext.builder()
                            .traceId(current.traceId()).clientApplication(current.clientApplication())
                            .apiRegistry(current.apiRegistry()).requiredPrivilegeCode(current.requiredPrivilegeCode())
                            .clientDecision(current.clientDecision()).clientDenyReason(current.clientDenyReason())
                            .userDecision(current.userDecision()).userDenyReason(current.userDenyReason())
                            .userId(user.userId())
                            .scopeAssignments(effectiveTenant.effectiveScopes())
                            .build());
                }
            }
            filterChain.doFilter(request, response);
        } catch (DataScopeAccessDeniedException exception) {
            AccessControlError error = AccessControlError.DATA_SCOPE_NOT_ALLOWED;
            responseWriter.writeError(response, error.getStatus(), error.name(), exception.getMessage());
        } finally {
            EffectiveTenantAccessContextHolder.clear();
            AuthenticatedRequestContextHolder.clear();
        }
    }
}
