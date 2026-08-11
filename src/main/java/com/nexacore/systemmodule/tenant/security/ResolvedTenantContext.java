package com.nexacore.systemmodule.tenant.security;

public record ResolvedTenantContext(long tenantId, String tenantCode, String hostname) {
}
