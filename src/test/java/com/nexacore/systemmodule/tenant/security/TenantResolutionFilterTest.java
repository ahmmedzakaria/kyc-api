package com.nexacore.systemmodule.tenant.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.service.HostnameNormalizer;
import com.nexacore.systemmodule.tenant.service.TenantDomainResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantResolutionFilterTest {
    private final AtomicReference<TenantDomainResolver.ResolvedTenant> resolved = new AtomicReference<>();
    private final HostnameNormalizer normalizer = new HostnameNormalizer();
    private final TenantDomainResolver resolver = new TenantDomainResolver(null, normalizer) {
        @Override public Optional<TenantDomainResolver.ResolvedTenant> resolveVerified(String rawHost) {
            return Optional.ofNullable(resolved.get());
        }
    };
    private final TenantResolutionFilter filter = new TenantResolutionFilter(
            resolver, new RequestTenantHostnameResolver(normalizer), new ApiResponseJsonWriter(new ObjectMapper()));

    @AfterEach void clear() { ResolvedTenantContextHolder.clear(); }

    @Test void resolvesActiveTenantAndClearsContextAfterRequest() throws Exception {
        resolved.set(tenant(TenantStatus.ACTIVE));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/private");
        request.setServerName("LOCALHOST");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ResolvedTenantContext> insideChain = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> insideChain.set(ResolvedTenantContextHolder.get().orElseThrow()));

        assertThat(insideChain.get().tenantId()).isEqualTo(1L);
        assertThat(response.getHeader("X-Tenant-Code")).isEqualTo("system");
        assertThat(ResolvedTenantContextHolder.get()).isEmpty();
    }

    @Test void rejectsUnknownAndSuspendedTenantsBeforeTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/private");
        request.setServerName("unknown.example");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TENANT_DOMAIN_NOT_RECOGNIZED");

        resolved.set(tenant(TenantStatus.SUSPENDED));
        response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("TENANT_NOT_ACTIVE");
    }

    @Test void resolvesCentralApiRequestFromBrowserOriginHostname() throws Exception {
        resolved.set(tenant(TenantStatus.ACTIVE));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/config");
        request.setServerName("localhost");
        request.addHeader("Origin", "http://bdcom.localhost:5301");
        AtomicReference<ResolvedTenantContext> insideChain = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> insideChain.set(ResolvedTenantContextHolder.get().orElseThrow()));

        assertThat(insideChain.get().hostname()).isEqualTo("bdcom.localhost");
    }

    private TenantDomainResolver.ResolvedTenant tenant(TenantStatus status) {
        return new TenantDomainResolver.ResolvedTenant(1L, "system", status);
    }
}
