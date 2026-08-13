package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.tenant.dto.*;
import com.nexacore.systemmodule.tenant.entity.*;
import com.nexacore.systemmodule.tenant.repository.TenantDomainRepository;
import com.nexacore.systemmodule.tenant.repository.TenantRepository;
import com.nexacore.systemmodule.tenant.repository.PlatformAdminAuditRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationTenantRepository;
import com.nexacore.systemmodule.layout.repository.ClientLayoutProfileRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.OptimisticLockingFailureException;

@Service
@RequiredArgsConstructor
public class TenantAdministrationService {
    private final TenantRepository tenantRepository;
    private final TenantDomainRepository domainRepository;
    private final TenantCodeNormalizer codeNormalizer;
    private final HostnameNormalizer hostnameNormalizer;
    private final TenantDomainResolver domainResolver;
    private final PlatformAdministrationAuditService auditService;
    private final PlatformAdminAuditRepository auditRepository;
    private final ClientApplicationTenantRepository clientTenantRepository;
    private final ClientLayoutProfileRepository clientLayoutRepository;
    private final LicenseSubscriptionRepository licenseSubscriptionRepository;
    private final AuthModuleGateway authModuleGateway;

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
        auditService.recordSuccess(actorId, tenant.getId(), "TENANT_REGISTER", code);
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
        auditService.recordSuccess(actorId, domain.getTenant().getId(), "TENANT_DOMAIN_VERIFY", domain.getHostname());
        return toResponse(domain.getTenant(), domain);
    }

    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public TenantDetailDto detail(long tenantId, long actorId) {
        auditService.recordAttempt(actorId, tenantId, "TENANT_DETAIL", null);
        SysTenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        List<TenantDomainDto> domains = domainRepository.findByTenantId(tenantId).stream().map(this::toDomainDto).toList();
        TenantAssignmentSummaryDto assignments = new TenantAssignmentSummaryDto(
                clientTenantRepository.countByTenantIdAndActiveTrue(tenantId),
                authModuleGateway.countUsersByTenantId(tenantId),
                clientLayoutRepository.countByTenantId(tenantId),
                licenseSubscriptionRepository.countByTenantId(tenantId),
                clientLayoutRepository.countBrandingOverrides(tenantId));
        List<PlatformAdminAuditEventDto> history = auditRepository.findByTargetTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .limit(100).map(event -> new PlatformAdminAuditEventDto(event.getId(), event.getActorUserId(), event.getActionCode(),
                        event.getOutcome(), event.getReason(), event.getTraceId(), event.getCreatedAt())).toList();
        return new TenantDetailDto(tenant.getId(), tenant.getVersion(), tenant.getTenantCode(), tenant.getDisplayName(),
                tenant.getLegalName(), tenant.getRegistrationNumber(), tenant.getBillingEmail(), tenant.getDefaultLocale(),
                tenant.getDefaultTimeZone(), tenant.getStatus(), tenant.getActivatedAt(), tenant.getSuspendedAt(),
                tenant.getSuspensionReason(), tenant.getCancelledAt(), tenant.getCancellationReason(), domains, assignments,
                history, tenant.getCreatedAt(), tenant.getUpdatedAt());
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantDomainDto addDomain(TenantDomainMutationRequest request, long actorId) {
        SysTenant tenant = tenantRepository.findById(request.tenantId()).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (tenant.getStatus() == TenantStatus.CANCELLED) throw new IllegalStateException("Cancelled tenant cannot receive domains");
        String hostname = hostnameNormalizer.normalize(request.hostname());
        auditService.recordAttempt(actorId, tenant.getId(), "TENANT_DOMAIN_ADD", hostname);
        if (domainRepository.existsByHostnameIgnoreCase(hostname)) throw new IllegalArgumentException("Tenant hostname already exists");
        SysTenantDomain domain = new SysTenantDomain();
        domain.setTenant(tenant); domain.setHostname(hostname);
        domain.setDomainType(request.domainType() == null ? TenantDomainType.CUSTOM : request.domainType());
        domain.setPrimaryDomain(domainRepository.findByTenantId(tenant.getId()).isEmpty());
        domain.setCreatedBy(actorId); domain.setUpdatedBy(actorId);
        domain = domainRepository.save(domain);
        auditService.recordSuccess(actorId, tenant.getId(), "TENANT_DOMAIN_ADD", hostname);
        return toDomainDto(domain);
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantDomainDto setPrimaryDomain(TenantDomainMutationRequest request, long actorId) {
        SysTenantDomain selected = requiredDomain(request);
        auditService.recordAttempt(actorId, request.tenantId(), "TENANT_DOMAIN_SET_PRIMARY", selected.getHostname());
        if (!selected.isActive() || selected.getVerificationStatus() != TenantDomainVerificationStatus.VERIFIED) {
            throw new IllegalStateException("Primary domain must be active and verified");
        }
        List<SysTenantDomain> domains = domainRepository.findByTenantId(request.tenantId());
        domains.forEach(domain -> { domain.setPrimaryDomain(domain.getId().equals(selected.getId())); domain.setUpdatedBy(actorId); });
        domainRepository.saveAll(domains);
        domainResolver.invalidateAll(domains.stream().map(SysTenantDomain::getHostname).toList());
        auditService.recordSuccess(actorId, request.tenantId(), "TENANT_DOMAIN_SET_PRIMARY", selected.getHostname());
        return toDomainDto(selected);
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantDomainDto deactivateDomain(TenantDomainMutationRequest request, long actorId) {
        SysTenantDomain domain = requiredDomain(request);
        auditService.recordAttempt(actorId, request.tenantId(), "TENANT_DOMAIN_DEACTIVATE", domain.getHostname());
        if (domain.isPrimaryDomain()) throw new IllegalStateException("Primary domain must be replaced before deactivation");
        domain.setActive(false); domain.setUpdatedBy(actorId); domain = domainRepository.save(domain);
        domainResolver.invalidate(domain.getHostname());
        auditService.recordSuccess(actorId, request.tenantId(), "TENANT_DOMAIN_DEACTIVATE", domain.getHostname());
        return toDomainDto(domain);
    }

    private SysTenantDomain requiredDomain(TenantDomainMutationRequest request) {
        if (request.domainId() == null) throw new IllegalArgumentException("domainId is required");
        return domainRepository.findByIdAndTenantId(request.domainId(), request.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant domain not found"));
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public TenantResponse transition(TenantLifecycleRequest request, TenantStatus target, long actorId) {
        auditService.recordAttempt(actorId, request.tenantId(), "TENANT_LIFECYCLE_" + target.name(), request.reason());
        SysTenant tenant = tenantRepository.findById(request.tenantId()).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        if (!Objects.equals(tenant.getVersion(), request.version())) {
            throw new OptimisticLockingFailureException("Tenant changed since it was loaded");
        }
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
        SysTenant saved = tenantRepository.saveAndFlush(tenant);
        var domains = domainRepository.findByTenantId(saved.getId());
        domainResolver.invalidateAll(domains.stream().map(SysTenantDomain::getHostname).toList());
        auditService.recordSuccess(actorId, saved.getId(), "TENANT_LIFECYCLE_" + target.name(), request.reason());
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
        return new TenantResponse(tenant.getId(), tenant.getVersion(), tenant.getTenantCode(), tenant.getDisplayName(), tenant.getStatus(),
                primaryDomain == null ? null : primaryDomain.getId(),
                primaryDomain == null ? null : primaryDomain.getHostname(), tenant.getCreatedAt(), tenant.getUpdatedAt());
    }

    private TenantDomainDto toDomainDto(SysTenantDomain domain) {
        return new TenantDomainDto(domain.getId(), domain.getHostname(), domain.getDomainType(), domain.getVerificationStatus(),
                domain.isPrimaryDomain(), domain.isActive(), domain.getVerifiedAt(), domain.getLastCheckedAt(),
                domain.getCreatedAt(), domain.getUpdatedAt());
    }
}
