package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.tenant.dto.*;
import com.nexacore.systemmodule.tenant.entity.*;
import com.nexacore.systemmodule.tenant.repository.TenantDomainRepository;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantAdministrationService {
    private final TenantRepository tenantRepository;
    private final TenantDomainRepository domainRepository;
    private final TenantCodeNormalizer codeNormalizer;
    private final HostnameNormalizer hostnameNormalizer;
    private final TenantDomainResolver domainResolver;
    private final PlatformAdministrationAuditService auditService;

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantResponse register(TenantRequest request, long actorId) {
        auditService.recordAttempt(actorId, null, "TENANT_REGISTER", request.tenantCode());
        String code = codeNormalizer.normalize(request.tenantCode());
        String hostname = hostnameNormalizer.normalize(request.hostname());
        if (tenantRepository.findByTenantCodeIgnoreCase(code).isPresent()) throw new IllegalArgumentException("Tenant code already exists");
        if (domainRepository.existsByHostnameIgnoreCase(hostname)) throw new IllegalArgumentException("Tenant hostname already exists");
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(code);
        tenant.setDisplayName(request.displayName().trim());
        tenant.setLegalName(request.legalName());
        tenant.setRegistrationNumber(request.registrationNumber());
        tenant.setBillingEmail(request.billingEmail());
        if (request.defaultLocale() != null && !request.defaultLocale().isBlank()) tenant.setDefaultLocale(request.defaultLocale().trim());
        if (request.defaultTimeZone() != null && !request.defaultTimeZone().isBlank()) tenant.setDefaultTimeZone(request.defaultTimeZone().trim());
        tenant.setCreatedBy(actorId);
        tenant.setUpdatedBy(actorId);
        tenant = tenantRepository.save(tenant);
        SysTenantDomain domain = new SysTenantDomain();
        domain.setTenant(tenant);
        domain.setHostname(hostname);
        domain.setDomainType(request.domainType() == null ? TenantDomainType.CUSTOM : request.domainType());
        domain.setPrimaryDomain(true);
        domain.setCreatedBy(actorId);
        domain.setUpdatedBy(actorId);
        domain = domainRepository.save(domain);
        return toResponse(tenant, domain);
    }

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<TenantResponse> list(long actorId) {
        auditService.recordAttempt(actorId, null, "TENANT_LIST_ALL", null);
        return tenantRepository.findAllByOrderByTenantCodeAsc().stream().map(t -> toResponse(t,
                domainRepository.findByTenantId(t.getId()).stream()
                        .filter(SysTenantDomain::isPrimaryDomain)
                        .findFirst().orElse(null))).toList();
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantResponse verifyDomain(long domainId, long actorId) {
        SysTenantDomain domain = domainRepository.findById(domainId).orElseThrow(() -> new IllegalArgumentException("Tenant domain not found"));
        auditService.recordAttempt(actorId, domain.getTenant().getId(), "TENANT_DOMAIN_VERIFY", domain.getHostname());
        domain.setVerificationStatus(TenantDomainVerificationStatus.VERIFIED);
        domain.setVerifiedAt(LocalDateTime.now());
        domain.setLastCheckedAt(LocalDateTime.now());
        domain.setUpdatedBy(actorId);
        domainRepository.save(domain);
        domainResolver.invalidate(domain.getHostname());
        return toResponse(domain.getTenant(), domain);
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantResponse transition(TenantLifecycleRequest request, TenantStatus target, long actorId) {
        auditService.recordAttempt(actorId, request.tenantId(), "TENANT_LIFECYCLE_" + target.name(), request.reason());
        SysTenant tenant = tenantRepository.findById(request.tenantId()).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        assertTransition(tenant.getStatus(), target);
        if (target == TenantStatus.ACTIVE) {
            boolean hasVerifiedDomain = domainRepository.findByTenantId(tenant.getId()).stream().anyMatch(d -> d.getTenant().getId().equals(tenant.getId())
                    && d.isActive() && d.getVerificationStatus() == TenantDomainVerificationStatus.VERIFIED);
            if (!hasVerifiedDomain) throw new IllegalStateException("A verified active domain is required before activation");
            tenant.setActivatedAt(LocalDateTime.now());
            tenant.setSuspendedAt(null); tenant.setSuspensionReason(null);
        } else if (target == TenantStatus.SUSPENDED) {
            requireReason(request.reason()); tenant.setSuspendedAt(LocalDateTime.now()); tenant.setSuspensionReason(request.reason().trim());
        } else if (target == TenantStatus.CANCELLED) {
            requireReason(request.reason()); tenant.setCancelledAt(LocalDateTime.now()); tenant.setCancellationReason(request.reason().trim());
        }
        tenant.setStatus(target);
        tenant.setUpdatedBy(actorId);
        SysTenant saved = tenantRepository.save(tenant);
        var domains = domainRepository.findByTenantId(saved.getId());
        domainResolver.invalidateAll(domains.stream().map(SysTenantDomain::getHostname).toList());
        return toResponse(saved, domains.stream().filter(SysTenantDomain::isPrimaryDomain)
                .findFirst().orElse(null));
    }

    private void assertTransition(TenantStatus from, TenantStatus to) {
        boolean allowed = (to == TenantStatus.ACTIVE && (from == TenantStatus.PENDING || from == TenantStatus.SUSPENDED))
                || (to == TenantStatus.SUSPENDED && from == TenantStatus.ACTIVE)
                || (to == TenantStatus.CANCELLED && from != TenantStatus.CANCELLED);
        if (!allowed) throw new IllegalStateException("Invalid tenant lifecycle transition: " + from + " -> " + to);
    }
    private void requireReason(String reason) { if (reason == null || reason.isBlank()) throw new IllegalArgumentException("A lifecycle reason is required"); }
    private TenantResponse toResponse(SysTenant tenant, SysTenantDomain primaryDomain) {
        return new TenantResponse(tenant.getId(), tenant.getTenantCode(), tenant.getDisplayName(), tenant.getStatus(),
                primaryDomain == null ? null : primaryDomain.getId(),
                primaryDomain == null ? null : primaryDomain.getHostname(), tenant.getCreatedAt(), tenant.getUpdatedAt());
    }
}
