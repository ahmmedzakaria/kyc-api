package com.nexacore.authmodule.core.dto;

import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record SsoContextDto(
        boolean enabled,
        String issuerUri,
        String clientId,
        String redirectUri,
        String logoutRedirectUri,
        List<String> scopes,
        boolean pkceRequired
) {
    public SsoContextDto {
        scopes = scopes == null ? new ArrayList<>() : scopes;
    }
}
