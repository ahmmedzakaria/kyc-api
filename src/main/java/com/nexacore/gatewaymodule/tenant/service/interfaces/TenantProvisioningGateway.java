package com.nexacore.gatewaymodule.tenant.service.interfaces;

public interface TenantProvisioningGateway {
    void requireActiveTenant(Long tenantId);
}
