package com.nexacore.authmodule.core.dto;

import com.nexacore.authmodule.core.enums.AuthenticationMode;
import lombok.Builder;

@Builder
public record AuthConfigResponse(
        AuthenticationMode authMode,
        String issuerUri,
        String clientId,
        String redirectUri
) {
}
