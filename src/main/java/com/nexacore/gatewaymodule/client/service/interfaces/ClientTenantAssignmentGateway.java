package com.nexacore.gatewaymodule.client.service.interfaces;

public interface ClientTenantAssignmentGateway {
    void requireActiveAssignment(String clientCode, Long tenantId);
}
