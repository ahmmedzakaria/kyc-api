package com.nexacore.systemmodule.tenant.service;

import com.nexacore.gatewaymodule.tenant.service.interfaces.TenantProvisioningGateway;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TenantProvisioningGatewayImpl implements TenantProvisioningGateway {
    private final TenantRepository tenantRepository;

    public TenantProvisioningGatewayImpl(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public void requireActiveTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0 || tenantRepository.findById(tenantId)
                .filter(tenant -> tenant.getStatus() == TenantStatus.ACTIVE).isEmpty()) {
            throw new IllegalArgumentException("Target tenant is not active");
        }
    }
}
