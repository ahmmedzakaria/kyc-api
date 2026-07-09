package com.nexacore.systemmodule.clientaccess.service.implementations;

import com.nexacore.systemmodule.clientaccess.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.clientaccess.entity.SysApiRegistry;
import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;
import com.nexacore.systemmodule.clientaccess.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.clientaccess.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.clientaccess.service.interfaces.ClientAccessDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientAccessDecisionServiceImpl implements ClientAccessDecisionService {

    private final ClientApiPermissionRepository clientApiPermissionRepository;
    private final ClientFeaturePermissionRepository clientFeaturePermissionRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ClientAccessDecisionDto decide(SysClientApplication clientApplication, SysApiRegistry apiRegistry) {
        if (apiRegistry == null || apiRegistry.isPublicApi()) {
            return ClientAccessDecisionDto.allowed(clientApplication, apiRegistry);
        }
        if (clientApplication == null) {
            return ClientAccessDecisionDto.denied("CLIENT_REQUIRED", null, apiRegistry);
        }
        boolean apiAllowed = clientApiPermissionRepository.existsByClientApplicationIdAndApiRegistryIdAndActiveTrue(
                clientApplication.getId(),
                apiRegistry.getId()
        );
        if (!apiAllowed) {
            return ClientAccessDecisionDto.denied("CLIENT_API_NOT_ALLOWED", clientApplication, apiRegistry);
        }
        if (apiRegistry.getRequiredPrivilegeCode() == null || apiRegistry.getRequiredPrivilegeCode().isBlank()) {
            return ClientAccessDecisionDto.allowed(clientApplication, apiRegistry);
        }
        boolean featureAllowed = clientFeaturePermissionRepository.existsByClientApplicationIdAndPrivilegePrivilegeCodeAndActiveTrue(
                clientApplication.getId(),
                apiRegistry.getRequiredPrivilegeCode()
        );
        return featureAllowed
                ? ClientAccessDecisionDto.allowed(clientApplication, apiRegistry)
                : ClientAccessDecisionDto.denied("CLIENT_FEATURE_NOT_ALLOWED", clientApplication, apiRegistry);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<String> filterPrivilegeCodesForClient(SysClientApplication clientApplication, Set<String> userPrivilegeCodes) {
        if (clientApplication == null || userPrivilegeCodes == null || userPrivilegeCodes.isEmpty()) {
            return userPrivilegeCodes == null ? Set.of() : userPrivilegeCodes;
        }

        Set<String> clientPrivilegeCodes = clientFeaturePermissionRepository
                .findActivePrivilegeCodesByClientApplicationId(clientApplication.getId());
        if (clientPrivilegeCodes.isEmpty()) {
            return Set.of();
        }

        return userPrivilegeCodes.stream()
                .filter(new HashSet<>(clientPrivilegeCodes)::contains)
                .collect(Collectors.toSet());
    }
}
