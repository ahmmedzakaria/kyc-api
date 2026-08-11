package com.nexacore.systemmodule.accesscontrol.security;

import java.util.Optional;

public final class EffectiveTenantAccessContextHolder {
    private static final ThreadLocal<EffectiveTenantAccessContext> CONTEXT = new ThreadLocal<>();
    private EffectiveTenantAccessContextHolder() {}
    public static void set(EffectiveTenantAccessContext context) { CONTEXT.set(context); }
    public static Optional<EffectiveTenantAccessContext> get() { return Optional.ofNullable(CONTEXT.get()); }
    public static void clear() { CONTEXT.remove(); }
}
