package com.nexacore.systemmodule.accesscontrol.security;

public class RateLimitBackendUnavailableException extends RuntimeException {
    public RateLimitBackendUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
