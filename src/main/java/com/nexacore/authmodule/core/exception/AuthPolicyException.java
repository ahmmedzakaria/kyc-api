package com.nexacore.authmodule.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthPolicyException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public AuthPolicyException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static AuthPolicyException notConfigured() {
        return new AuthPolicyException(HttpStatus.FORBIDDEN, "AUTH_POLICY_NOT_CONFIGURED",
                "Authentication policy is not configured for this tenant and client");
    }

    public static AuthPolicyException methodNotAllowed() {
        return new AuthPolicyException(HttpStatus.FORBIDDEN, "LOGIN_METHOD_NOT_ALLOWED",
                "The requested login method is not allowed");
    }

    public static AuthPolicyException integrityError() {
        return new AuthPolicyException(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_POLICY_INTEGRITY_ERROR",
                "Authentication policy integrity validation failed");
    }

    public static AuthPolicyException clientContextRequired() {
        return new AuthPolicyException(HttpStatus.BAD_REQUEST, "CLIENT_CONTEXT_REQUIRED",
                "Client context is required");
    }

    public static AuthPolicyException clientContextNotAllowed() {
        return new AuthPolicyException(HttpStatus.FORBIDDEN, "CLIENT_CONTEXT_NOT_ALLOWED",
                "Client context is not allowed");
    }
}
