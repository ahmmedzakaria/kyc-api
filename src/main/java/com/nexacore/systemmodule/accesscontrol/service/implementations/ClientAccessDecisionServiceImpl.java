package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.dto.ClientAccessDecisionDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApiPermissionRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientFeaturePermissionRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientAccessDecisionService;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
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
    private final AuthorizationDataCache authorizationDataCache;

    private AuthorizationDataCache.ClientGrantSnapshot grants(Long clientId) {
        return authorizationDataCache.clientGrants(clientId, () -> new AuthorizationDataCache.ClientGrantSnapshot(
                clientApiPermissionRepository.findActiveApiRegistryIdsByClientApplicationId(clientId),
                clientFeaturePermissionRepository.findActivePrivilegeCodesByClientApplicationId(clientId)));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ClientAccessDecisionDto decide(SysAccClientApplication clientApplication, SysAccApiRegistry apiRegistry) {
        if (apiRegistry == null || apiRegistry.isPublicApi()) {
            return ClientAccessDecisionDto.allowed(clientApplication, apiRegistry);
        }
        if (clientApplication == null) {
            return ClientAccessDecisionDto.denied("CLIENT_REQUIRED", null, apiRegistry);
        }
        AuthorizationDataCache.ClientGrantSnapshot grants = grants(clientApplication.getId());
        boolean apiAllowed = grants.apiIds().contains(apiRegistry.getId());
        if (!apiAllowed) {
            return ClientAccessDecisionDto.denied("CLIENT_API_NOT_ALLOWED", clientApplication, apiRegistry);
        }
        if (apiRegistry.getRequiredPrivilegeCode() == null || apiRegistry.getRequiredPrivilegeCode().isBlank()) {
            return ClientAccessDecisionDto.allowed(clientApplication, apiRegistry);
        }
        boolean featureAllowed = grants.privileges().contains(apiRegistry.getRequiredPrivilegeCode());
        return featureAllowed
                ? ClientAccessDecisionDto.allowed(clientApplication, apiRegistry)
                : ClientAccessDecisionDto.denied("CLIENT_FEATURE_NOT_ALLOWED", clientApplication, apiRegistry);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Set<String> filterPrivilegeCodesForClient(SysAccClientApplication clientApplication, Set<String> userPrivilegeCodes) {
        if (clientApplication == null || userPrivilegeCodes == null || userPrivilegeCodes.isEmpty()) {
            return userPrivilegeCodes == null ? Set.of() : userPrivilegeCodes;
        }

        Set<String> clientPrivilegeCodes = grants(clientApplication.getId()).privileges();
        if (clientPrivilegeCodes.isEmpty()) {
            return Set.of();
        }

        return userPrivilegeCodes.stream()
                .filter(new HashSet<>(clientPrivilegeCodes)::contains)
                .collect(Collectors.toSet());
    }
}
