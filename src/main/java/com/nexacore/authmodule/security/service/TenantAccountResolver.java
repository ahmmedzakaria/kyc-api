package com.nexacore.authmodule.security.service;

import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.TenantDomainResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class TenantAccountResolver {
    private final ClientCredentialService clientCredentialService;
    private final ClientApplicationTenantRepository clientTenantRepository;
    private final TenantDomainResolver tenantDomainResolver;

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Long resolveRequiredTenant(String clientCode) {
        if (!StringUtils.hasText(clientCode)) {
            throw new BadCredentialsException("TENANT_CONTEXT_REQUIRED");
        }
        var client = clientCredentialService.resolveActiveClient(clientCode.trim())
                .orElseThrow(() -> new BadCredentialsException("INVALID_CLIENT_CONTEXT"));
        HttpServletRequest request = RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
        if (request == null) throw new BadCredentialsException("TENANT_CONTEXT_REQUIRED");
        var tenant = tenantDomainResolver.resolveVerified(request.getServerName())
                .orElseThrow(() -> new BadCredentialsException("TENANT_DOMAIN_INVALID"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) throw new BadCredentialsException("TENANT_NOT_ACTIVE");
        if (!clientTenantRepository.existsByClientApplicationIdAndTenantIdAndActiveTrue(client.getId(), tenant.getId())) {
            throw new BadCredentialsException("CLIENT_TENANT_NOT_ASSIGNED");
        }
        return tenant.getId();
    }
}
