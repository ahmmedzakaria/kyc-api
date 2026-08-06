package com.nexacore.systemmodule.accesscontrol.security;

public class ApiRouteAmbiguityException extends RuntimeException {
    public ApiRouteAmbiguityException(String message) {
        super(message);
    }
}
