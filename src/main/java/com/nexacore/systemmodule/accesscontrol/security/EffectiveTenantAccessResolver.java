package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EffectiveTenantAccessResolver {
    private final HostnameNormalizer hostnameNormalizer;
    private final ClientApplicationTenantRepository clientTenantRepository;

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public EffectiveTenantAccessContext resolve(String rawHost, long accountId, long accountTenantId,
                                                SysAccClientApplication client,
                                                Set<UserScopeAssignment> accountScopes) {
        if (client == null) throw denied("A registered client is required for tenant access");
        String hostname = hostnameNormalizer.normalize(rawHost);
        var resolved = ResolvedTenantContextHolder.get()
                .orElseThrow(() -> denied("A verified request tenant is required"));
        if (!resolved.hostname().equals(hostname)) throw denied("The resolved tenant context is contradictory");
        if (resolved.tenantId() != accountTenantId) throw denied("The authenticated account belongs to another tenant");
        if (!clientTenantRepository.existsByClientApplicationIdAndTenantIdAndActiveTrue(client.getId(), resolved.tenantId())) {
            throw denied("The client is not assigned to the resolved tenant");
        }
        Set<UserScopeAssignment> effectiveScopes = accountScopes == null ? Set.of() : accountScopes.stream()
                .filter(scope -> scope.tenantId().equals(resolved.tenantId()))
                .collect(Collectors.toUnmodifiableSet());
        if (effectiveScopes.isEmpty()) throw denied("No active account scope covers the resolved tenant");
        return new EffectiveTenantAccessContext(resolved.tenantId(), resolved.tenantCode(), hostname,
                accountId, client.getId(), effectiveScopes);
    }

    private DataScopeAccessDeniedException denied(String message) {
        return new DataScopeAccessDeniedException(message);
    }
}
