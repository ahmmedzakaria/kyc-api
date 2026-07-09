package com.nexacore.systemmodule.clientaccess.security;

import java.util.Optional;

public final class ClientApplicationContextHolder {

    private static final ThreadLocal<ClientApplicationContext> CONTEXT = new ThreadLocal<>();

    private ClientApplicationContextHolder() {
    }

    public static void set(ClientApplicationContext context) {
        CONTEXT.set(context);
    }

    public static Optional<ClientApplicationContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
