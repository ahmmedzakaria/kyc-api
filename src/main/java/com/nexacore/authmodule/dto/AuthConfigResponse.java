package com.nexacore.authmodule.dto;

import com.nexacore.authmodule.enums.AuthenticationMode;
import lombok.Builder;

@Builder
public record AuthConfigResponse(
        AuthenticationMode authMode,
        String issuerUri,
        String clientId,
        String redirectUri
) {
}
