package com.nexacore.systemmodule.accesscontrol.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AccessControlError {
    AUTHENTICATION_REQUIRED(401, "Authentication is required"),
    INVALID_TOKEN_TYPE(401, "The supplied token type cannot access this resource"),
    INVALID_CLIENT_CREDENTIALS(401, "Valid confidential-client credentials are required"),
    CLIENT_ORIGIN_NOT_ALLOWED(403, "The request origin is not allowed for this client"),
    CLIENT_IP_NOT_ALLOWED(403, "The request IP address is not allowed for this client"),
    CLIENT_API_NOT_ALLOWED(403, "The client is not allowed to access this API"),
    CLIENT_FEATURE_NOT_ALLOWED(403, "The client is not allowed to access this feature"),
    USER_PRIVILEGE_NOT_ALLOWED(403, "The user does not have the required privilege"),
    DATA_SCOPE_NOT_ALLOWED(403, "The requested data is outside the allowed scope"),
    API_NOT_REGISTERED(403, "The protected API has no registry metadata");

    private final int status;
    private final String message;

    public static AccessControlError fromClientDecision(String denyReason) {
        if ("CLIENT_REQUIRED".equals(denyReason) || "INVALID_CLIENT_CREDENTIALS".equals(denyReason)) {
            return INVALID_CLIENT_CREDENTIALS;
        }
        try {
            return valueOf(denyReason);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return CLIENT_API_NOT_ALLOWED;
        }
    }
}
