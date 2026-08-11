package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import com.nexacore.systemmodule.tenant.service.TenantDomainResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EffectiveTenantAccessResolver {
    private final TenantDomainResolver domainResolver;
    private final HostnameNormalizer hostnameNormalizer;
    private final ClientApplicationTenantRepository clientTenantRepository;

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public EffectiveTenantAccessContext resolve(String rawHost, long accountId, long accountTenantId,
                                                SysAccClientApplication client,
                                                Set<UserScopeAssignment> accountScopes) {
        if (client == null) throw denied("A registered client is required for tenant access");
        String hostname = hostnameNormalizer.normalize(rawHost);
        var tenant = domainResolver.resolveVerified(hostname)
                .orElseThrow(() -> denied("The request hostname is not assigned to a verified tenant"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) throw denied("The resolved tenant is not active");
        if (tenant.getId() != accountTenantId) throw denied("The authenticated account belongs to another tenant");
        if (!clientTenantRepository.existsByClientApplicationIdAndTenantIdAndActiveTrue(client.getId(), tenant.getId())) {
            throw denied("The client is not assigned to the resolved tenant");
        }
        Set<UserScopeAssignment> effectiveScopes = accountScopes == null ? Set.of() : accountScopes.stream()
                .filter(scope -> scope.tenantId().equals(tenant.getId()))
                .collect(Collectors.toUnmodifiableSet());
        if (effectiveScopes.isEmpty()) throw denied("No active account scope covers the resolved tenant");
        return new EffectiveTenantAccessContext(tenant.getId(), tenant.getTenantCode(), hostname,
                accountId, client.getId(), effectiveScopes);
    }

    private DataScopeAccessDeniedException denied(String message) {
        return new DataScopeAccessDeniedException(message);
    }
}
