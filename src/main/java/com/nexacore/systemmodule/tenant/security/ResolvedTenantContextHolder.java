package com.nexacore.systemmodule.tenant.security;

import java.util.Optional;

public final class ResolvedTenantContextHolder {
    private static final ThreadLocal<ResolvedTenantContext> CONTEXT = new ThreadLocal<>();

    private ResolvedTenantContextHolder() {
    }

    public static void set(ResolvedTenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<ResolvedTenantContext> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
