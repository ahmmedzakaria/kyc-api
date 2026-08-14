package com.nexacore.gatewaymodule.client.dto;

public record ClientSsoConfigurationDto(
        String clientCode,
        String oauthClientId,
        String redirectUri,
        String logoutRedirectUri
) {
}
