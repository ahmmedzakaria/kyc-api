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
    public Resolution resolve(String rawHost) {
        String hostname = hostnameNormalizer.normalize(rawHost);
        CachedTenant cached = cache.get(hostname);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Resolution.resolved(cached.tenant());
        }
        cache.remove(hostname);
        var domains = repository.findTop2ByHostnameAndActiveTrueAndVerificationStatusOrderByIdAsc(
                hostname, TenantDomainVerificationStatus.VERIFIED);
        if (domains.isEmpty()) return Resolution.unknown();
        if (domains.size() != 1) return Resolution.ambiguous();
        var tenant = domains.getFirst().getTenant();
        var resolved = new ResolvedTenant(tenant.getId(), tenant.getTenantCode(), tenant.getStatus());
        if (resolved.status() == TenantStatus.ACTIVE) {
            cache.put(hostname, new CachedTenant(resolved, Instant.now().plus(TTL)));
        }
        return Resolution.resolved(resolved);
    }

    /** Compatibility contract for callers that do not need the denial reason. */
    public Optional<ResolvedTenant> resolveVerified(String rawHost) {
        Resolution resolution = resolve(rawHost);
        return resolution.status() == ResolutionStatus.RESOLVED
                ? Optional.of(resolution.tenant()) : Optional.empty();
    }

    public void invalidate(String rawHost) {
        cache.remove(hostnameNormalizer.normalize(rawHost));
    }

    public void invalidateAll(Iterable<String> hostnames) {
        hostnames.forEach(this::invalidate);
    }

    public record ResolvedTenant(Long id, String tenantCode, TenantStatus status) {}

    public enum ResolutionStatus { RESOLVED, UNKNOWN, AMBIGUOUS }

    public record Resolution(ResolutionStatus status, ResolvedTenant tenant) {
        public static Resolution resolved(ResolvedTenant tenant) {
            return new Resolution(ResolutionStatus.RESOLVED, tenant);
        }
        public static Resolution unknown() {
            return new Resolution(ResolutionStatus.UNKNOWN, null);
        }
        public static Resolution ambiguous() {
            return new Resolution(ResolutionStatus.AMBIGUOUS, null);
        }
    }

    private record CachedTenant(ResolvedTenant tenant, Instant expiresAt) {}
}
