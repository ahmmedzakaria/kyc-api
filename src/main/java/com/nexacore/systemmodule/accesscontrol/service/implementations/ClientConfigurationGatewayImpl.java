package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.client.dto.ClientSsoConfigurationDto;
import com.nexacore.gatewaymodule.client.service.interfaces.ClientConfigurationGateway;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.security.ClientOriginPolicy;
import com.nexacore.systemmodule.accesscontrol.security.ClientRedirectUriPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ClientConfigurationGatewayImpl implements ClientConfigurationGateway {
    private final ClientApplicationRepository repository;
    private final ClientOriginPolicy originPolicy;
    private final ClientRedirectUriPolicy redirectUriPolicy;

    public ClientConfigurationGatewayImpl(ClientApplicationRepository repository,
                                          ClientOriginPolicy originPolicy,
                                          ClientRedirectUriPolicy redirectUriPolicy) {
        this.repository = repository;
        this.originPolicy = originPolicy;
        this.redirectUriPolicy = redirectUriPolicy;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public ClientSsoConfigurationDto resolveSsoConfiguration(String clientCode, String requestOrigin) {
        if (!StringUtils.hasText(clientCode)) {
            throw new IllegalArgumentException("CLIENT_CONTEXT_REQUIRED");
        }
        SysAccClientApplication client = repository.findByClientCode(clientCode.trim())
                .filter(value -> value.getStatus() == ClientApplicationStatus.ACTIVE)
                .orElseThrow(() -> new IllegalArgumentException("CLIENT_CONTEXT_NOT_ALLOWED"));
        if (StringUtils.hasText(requestOrigin) && !originPolicy.isAllowed(client, requestOrigin)) {
            throw new IllegalArgumentException("CLIENT_CONTEXT_NOT_ALLOWED");
        }
        if (!StringUtils.hasText(client.getOauthClientId())) {
            throw new IllegalArgumentException("CLIENT_CONTEXT_NOT_ALLOWED");
        }
        try {
            return new ClientSsoConfigurationDto(
                    client.getClientCode(),
                    client.getOauthClientId(),
                    redirectUriPolicy.resolveRegisteredUri(client.getAllowedRedirectUris(), requestOrigin),
                    redirectUriPolicy.resolveRegisteredUri(client.getAllowedLogoutRedirectUris(), requestOrigin));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("CLIENT_CONTEXT_NOT_ALLOWED", exception);
        }
    }
}
