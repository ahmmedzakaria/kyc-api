package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.lang.reflect.Proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class EffectiveTenantAccessResolverTest {
    private final ClientApplicationTenantRepository assignments = (ClientApplicationTenantRepository) Proxy.newProxyInstance(
            ClientApplicationTenantRepository.class.getClassLoader(), new Class<?>[]{ClientApplicationTenantRepository.class},
            (proxy, method, args) -> method.getName().equals("existsByClientApplicationIdAndTenantIdAndActiveTrue"));
    private final EffectiveTenantAccessResolver resolver = new EffectiveTenantAccessResolver(new HostnameNormalizer(), assignments);

    @AfterEach void clear() { ResolvedTenantContextHolder.clear(); }

    @Test void returnsOnlyScopesInTheResolvedAccountTenant() {
        SysAccClientApplication client = new SysAccClientApplication(); client.setId(3L);
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(11L, "acme", "acme.example"));

        EffectiveTenantAccessContext context = resolver.resolve("ACME.EXAMPLE:443", 7L, 11L, client,
                Set.of(new UserScopeAssignment(11L, 20L, null)));

        assertThat(context.tenantId()).isEqualTo(11L);
        assertThat(context.effectiveScopes()).containsExactly(new UserScopeAssignment(11L, 20L, null));
    }

    @Test void rejectsCrossTenantTokenReplayAndMissingFilterContext() {
        SysAccClientApplication client = new SysAccClientApplication(); client.setId(3L);
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(11L, "acme", "acme.example"));
        assertThatThrownBy(() -> resolver.resolve("acme.example", 7L, 12L, client,
                Set.of(new UserScopeAssignment(12L, null, null))))
                .isInstanceOf(DataScopeAccessDeniedException.class).hasMessageContaining("another tenant");

        ResolvedTenantContextHolder.clear();
        assertThatThrownBy(() -> resolver.resolve("acme.example", 7L, 11L, client,
                Set.of(new UserScopeAssignment(11L, null, null))))
                .isInstanceOf(DataScopeAccessDeniedException.class).hasMessageContaining("verified request tenant");
    }
}
