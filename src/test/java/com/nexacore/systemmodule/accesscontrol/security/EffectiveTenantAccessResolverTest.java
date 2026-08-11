package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.tenant.entity.SysTenant;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import com.nexacore.systemmodule.tenant.service.TenantDomainResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class EffectiveTenantAccessResolverTest {
    private final AtomicReference<SysTenant> resolvedTenant = new AtomicReference<>();
    private final TenantDomainResolver domains = new TenantDomainResolver(null, new HostnameNormalizer()) {
        @Override public Optional<SysTenant> resolveVerified(String rawHost) { return Optional.ofNullable(resolvedTenant.get()); }
    };
    private final ClientApplicationTenantRepository assignments = (ClientApplicationTenantRepository) Proxy.newProxyInstance(
            ClientApplicationTenantRepository.class.getClassLoader(), new Class<?>[]{ClientApplicationTenantRepository.class},
            (proxy, method, args) -> method.getName().equals("existsByClientApplicationIdAndTenantIdAndActiveTrue"));
    private final EffectiveTenantAccessResolver resolver = new EffectiveTenantAccessResolver(domains, new HostnameNormalizer(), assignments);

    @Test void returnsOnlyScopesInTheResolvedAccountTenant() {
        SysTenant tenant = tenant(11L, TenantStatus.ACTIVE);
        SysAccClientApplication client = new SysAccClientApplication(); client.setId(3L);
        resolvedTenant.set(tenant);

        EffectiveTenantAccessContext context = resolver.resolve("ACME.EXAMPLE:443", 7L, 11L, client,
                Set.of(new UserScopeAssignment(11L, 20L, null)));

        assertThat(context.tenantId()).isEqualTo(11L);
        assertThat(context.effectiveScopes()).containsExactly(new UserScopeAssignment(11L, 20L, null));
    }

    @Test void rejectsCrossTenantTokenReplayAndInactiveTenant() {
        SysAccClientApplication client = new SysAccClientApplication(); client.setId(3L);
        resolvedTenant.set(tenant(11L, TenantStatus.ACTIVE));
        assertThatThrownBy(() -> resolver.resolve("acme.example", 7L, 12L, client,
                Set.of(new UserScopeAssignment(12L, null, null))))
                .isInstanceOf(DataScopeAccessDeniedException.class).hasMessageContaining("another tenant");

        resolvedTenant.set(tenant(11L, TenantStatus.SUSPENDED));
        assertThatThrownBy(() -> resolver.resolve("acme.example", 7L, 11L, client,
                Set.of(new UserScopeAssignment(11L, null, null))))
                .isInstanceOf(DataScopeAccessDeniedException.class).hasMessageContaining("not active");
    }

    private SysTenant tenant(long id, TenantStatus status) {
        SysTenant tenant = new SysTenant(); tenant.setId(id); tenant.setTenantCode("acme"); tenant.setStatus(status); return tenant;
    }
}
