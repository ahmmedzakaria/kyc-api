package com.nexacore.authmodule.security.service;

import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TenantAccountResolver {
    private final ClientCredentialService clientCredentialService;
    private final ClientApplicationTenantRepository clientTenantRepository;

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Long resolveRequiredTenant(String clientCode) {
        if (!StringUtils.hasText(clientCode)) {
            throw new BadCredentialsException("TENANT_CONTEXT_REQUIRED");
        }
        var client = clientCredentialService.resolveActiveClient(clientCode.trim())
                .orElseThrow(() -> new BadCredentialsException("INVALID_CLIENT_CONTEXT"));
        var tenantIds = clientTenantRepository.findByClientApplicationIdAndActiveTrue(client.getId()).stream()
                .map(assignment -> assignment.getTenantId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (tenantIds.size() != 1) {
            throw new BadCredentialsException("TENANT_CONTEXT_AMBIGUOUS");
        }
        return tenantIds.getFirst();
    }
}
