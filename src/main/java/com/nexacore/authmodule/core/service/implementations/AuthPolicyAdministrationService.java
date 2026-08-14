package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.dto.AuthPolicyAdministrationDto;
import com.nexacore.authmodule.core.dto.AuthPolicyAdministrationRequest;
import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.gatewaymodule.client.service.interfaces.ClientTenantAssignmentGateway;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContext;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class AuthPolicyAdministrationService {
    private final AuthClientAuthPolicyRepository repository;
    private final ClientTenantAssignmentGateway assignmentGateway;

    public AuthPolicyAdministrationService(AuthClientAuthPolicyRepository repository,
                                           ClientTenantAssignmentGateway assignmentGateway) {
        this.repository = repository;
        this.assignmentGateway = assignmentGateway;
    }

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public List<AuthPolicyAdministrationDto> list(Long tenantId) {
        requireTenantScope(tenantId);
        return repository.findAllByTenantIdOrderByClientCodeAsc(tenantId).stream().map(this::toDto).toList();
    }

    @Transactional(transactionManager = "authTransactionManager")
    public AuthPolicyAdministrationDto save(AuthPolicyAdministrationRequest request) {
        if (request == null || request.tenantId() == null || !StringUtils.hasText(request.clientCode())
                || request.loginMethod() == null || request.loginIdentifierType() == null) {
            throw new IllegalArgumentException("Tenant, client, login method, and identifier are required");
        }
        AuthenticatedRequestContext context = requireTenantScope(request.tenantId());
        assignmentGateway.requireActiveAssignment(request.clientCode(), request.tenantId());
        validateCompatibility(request.loginMethod(), request.loginIdentifierType());

        String clientCode = request.clientCode().trim().toUpperCase(java.util.Locale.ROOT);
        AuthClientAuthPolicy policy = repository.findByTenantIdAndClientCodeIgnoreCase(request.tenantId(), clientCode)
                .orElseGet(AuthClientAuthPolicy::new);
        policy.setTenantId(request.tenantId());
        policy.setClientCode(clientCode);
        policy.setLoginMethod(request.loginMethod());
        policy.setLoginIdentifierType(request.loginIdentifierType());
        policy.setEnabled(request.enabled() == null || request.enabled());
        if (policy.getId() == null) policy.setCreatedBy(context.userId());
        policy.setUpdatedBy(context.userId());
        return toDto(repository.saveAndFlush(policy));
    }

    private AuthenticatedRequestContext requireTenantScope(Long tenantId) {
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("Tenant scope is required");
        AuthenticatedRequestContext context = AuthenticatedRequestContextHolder.get()
                .orElseThrow(() -> new IllegalArgumentException("Authenticated request context is required"));
        boolean allowed = context.scopeAssignments().stream().anyMatch(scope -> tenantId.equals(scope.tenantId()));
        if (!allowed) throw new IllegalArgumentException("Tenant scope is not allowed");
        return context;
    }

    private void validateCompatibility(LoginMethod method, LoginIdentifierType identifier) {
        Set<LoginIdentifierType> allowed = switch (method) {
            case PASSWORD -> Set.of(LoginIdentifierType.USERNAME, LoginIdentifierType.EMAIL,
                    LoginIdentifierType.MOBILE, LoginIdentifierType.PERSON_ID);
            case OTP -> Set.of(LoginIdentifierType.MOBILE, LoginIdentifierType.EMAIL);
            case MAGIC_LINK -> Set.of(LoginIdentifierType.EMAIL);
            case SSO, OAUTH, PASSKEY, BIOMETRIC -> Set.of(LoginIdentifierType.USERNAME);
            case MFA -> Set.of();
        };
        if (!allowed.contains(identifier)) throw new IllegalArgumentException("Login method and identifier are incompatible");
    }

    private AuthPolicyAdministrationDto toDto(AuthClientAuthPolicy policy) {
        return new AuthPolicyAdministrationDto(policy.getId(), policy.getTenantId(), policy.getClientCode(),
                policy.getLoginMethod(), policy.getLoginIdentifierType(), policy.isEnabled(),
                policy.getId() + ":" + (policy.getUpdatedAt() == null ? "0" : policy.getUpdatedAt()), policy.getUpdatedAt());
    }
}
