package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Optional;

public final class AuthenticatedRequestContextHolder {
    private static final ThreadLocal<AuthenticatedRequestContext> CONTEXT = new ThreadLocal<>();

    private AuthenticatedRequestContextHolder() {
    }

    public static void set(AuthenticatedRequestContext context) {
        CONTEXT.set(context);
    }

    public static Optional<AuthenticatedRequestContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
