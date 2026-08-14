package com.nexacore.gatewaymodule.client.service.interfaces;

import com.nexacore.gatewaymodule.client.dto.ClientSsoConfigurationDto;

public interface ClientConfigurationGateway {
    ClientSsoConfigurationDto resolveSsoConfiguration(String clientCode, String requestOrigin);
}
