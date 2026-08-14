package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.client.service.interfaces.ClientTenantAssignmentGateway;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class ClientTenantAssignmentGatewayImpl implements ClientTenantAssignmentGateway {
    private final ClientApplicationRepository clientRepository;
    private final ClientApplicationTenantRepository assignmentRepository;

    public ClientTenantAssignmentGatewayImpl(ClientApplicationRepository clientRepository,
                                             ClientApplicationTenantRepository assignmentRepository) {
        this.clientRepository = clientRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public void requireActiveAssignment(String clientCode, Long tenantId) {
        if (!StringUtils.hasText(clientCode) || tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant and client assignment is not allowed");
        }
        var client = clientRepository.findByClientCode(clientCode.trim())
                .filter(value -> value.getStatus() == ClientApplicationStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("Tenant and client assignment is not allowed"));
        if (!assignmentRepository.existsByClientApplicationIdAndTenantIdAndActiveTrue(client.getId(), tenantId)) {
            throw new IllegalArgumentException("Tenant and client assignment is not allowed");
        }
    }
}
