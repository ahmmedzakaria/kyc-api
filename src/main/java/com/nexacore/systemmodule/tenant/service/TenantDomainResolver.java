package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.tenant.entity.TenantDomainVerificationStatus;
import com.nexacore.systemmodule.tenant.entity.TenantStatus;
import com.nexacore.systemmodule.tenant.repository.TenantDomainRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TenantDomainResolver {
    private static final Duration TTL = Duration.ofMinutes(5);
    private final TenantDomainRepository repository;
    private final HostnameNormalizer hostnameNormalizer;
    private final ConcurrentHashMap<String, CachedTenant> cache = new ConcurrentHashMap<>();

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<ResolvedTenant> resolveVerified(String rawHost) {
        String hostname = hostnameNormalizer.normalize(rawHost);
        CachedTenant cached = cache.get(hostname);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) return Optional.of(cached.tenant());
        cache.remove(hostname);
        Optional<ResolvedTenant> resolved = repository.findByHostnameAndActiveTrueAndVerificationStatus(
                        hostname, TenantDomainVerificationStatus.VERIFIED)
                .map(domain -> {
                    var tenant = domain.getTenant();
                    return new ResolvedTenant(tenant.getId(), tenant.getTenantCode(), tenant.getStatus());
                });
        resolved.ifPresent(tenant -> cache.put(hostname, new CachedTenant(tenant, Instant.now().plus(TTL))));
        return resolved;
    }

    public void invalidate(String rawHost) {
        cache.remove(hostnameNormalizer.normalize(rawHost));
    }

    public void invalidateAll(Iterable<String> hostnames) {
        hostnames.forEach(this::invalidate);
    }

    public record ResolvedTenant(Long id, String tenantCode, TenantStatus status) {}

    private record CachedTenant(ResolvedTenant tenant, Instant expiresAt) {}
}
