package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.tenant.entity.SysTenant;
import com.nexacore.systemmodule.tenant.entity.SysTenantDomain;
import com.nexacore.systemmodule.tenant.entity.TenantDomainVerificationStatus;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.TenantDomainRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantDomainResolverTest {
    private final TenantDomainRepository repository = mock(TenantDomainRepository.class);
    private final TenantDomainResolver resolver = new TenantDomainResolver(repository, new HostnameNormalizer());

    @Test
    void returnsUnknownWhenNoActiveVerifiedDomainMatches() {
        when(repository.findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "unknown.example", TenantDomainVerificationStatus.VERIFIED)).thenReturn(List.of());

        var result = resolver.resolve("UNKNOWN.example:443");

        assertThat(result.status()).isEqualTo(TenantDomainResolver.ResolutionStatus.UNKNOWN);
        assertThat(result.tenant()).isNull();
    }

    @Test
    void returnsAmbiguousWhenMoreThanOneDomainMatches() {
        when(repository.findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "duplicate.example", TenantDomainVerificationStatus.VERIFIED))
                .thenReturn(List.of(domain(1, TenantStatus.ACTIVE), domain(2, TenantStatus.ACTIVE)));

        var result = resolver.resolve("duplicate.example");

        assertThat(result.status()).isEqualTo(TenantDomainResolver.ResolutionStatus.AMBIGUOUS);
        assertThat(result.tenant()).isNull();
    }

    @Test
    void cachesOnlyUniqueActiveTenantMappings() {
        var activeDomain = domain(7, TenantStatus.ACTIVE);
        when(repository.findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "active.example", TenantDomainVerificationStatus.VERIFIED)).thenReturn(List.of(activeDomain));

        assertThat(resolver.resolve("active.example").tenant().id()).isEqualTo(7L);
        assertThat(resolver.resolve("ACTIVE.EXAMPLE").tenant().id()).isEqualTo(7L);

        verify(repository, times(1)).findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "active.example", TenantDomainVerificationStatus.VERIFIED);
    }

    @Test
    void doesNotCacheInactiveTenantMappings() {
        var suspendedDomain = domain(9, TenantStatus.SUSPENDED);
        when(repository.findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "suspended.example", TenantDomainVerificationStatus.VERIFIED)).thenReturn(List.of(suspendedDomain));

        assertThat(resolver.resolve("suspended.example").tenant().status()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(resolver.resolve("suspended.example").tenant().status()).isEqualTo(TenantStatus.SUSPENDED);

        verify(repository, times(2)).findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                "suspended.example", TenantDomainVerificationStatus.VERIFIED);
    }

    private SysTenantDomain domain(long tenantId, TenantStatus status) {
        SysTenant tenant = new SysTenant();
        tenant.setId(tenantId);
        tenant.setTenantCode("tenant-" + tenantId);
        tenant.setStatus(status);
        SysTenantDomain domain = new SysTenantDomain();
        domain.setTenant(tenant);
        return domain;
    }
}
