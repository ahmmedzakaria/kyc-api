package com.nexacore.systemmodule.tenant.service;

import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;
import com.nexacore.systemmodule.tenant.entity.SysPlatformAdminAuditEvent;
import com.nexacore.systemmodule.tenant.repository.PlatformAdminAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformAdministrationAuditService {
    private final PlatformAdminAuditRepository repository;

    @Transactional(transactionManager = "systemTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void recordAttempt(long actorId, Long targetTenantId, String actionCode, String reason) {
        record(actorId, targetTenantId, actionCode, "ATTEMPT", reason);
    }

    @Transactional(transactionManager = "systemTransactionManager")
    public void recordSuccess(long actorId, Long targetTenantId, String actionCode, String reason) {
        record(actorId, targetTenantId, actionCode, "SUCCESS", reason);
    }

    private void record(long actorId, Long targetTenantId, String actionCode, String outcome, String reason) {
        var requestContext = AuthenticatedRequestContextHolder.get().orElse(null);
        SysPlatformAdminAuditEvent event = new SysPlatformAdminAuditEvent();
        event.setActorUserId(actorId);
        event.setActorTenantId(requestContext == null ? null : requestContext.scopeAssignments().stream()
                .map(scope -> scope.tenantId()).distinct().filter(id -> id != null).findFirst().orElse(null));
        event.setTargetTenantId(targetTenantId);
        event.setActionCode(actionCode);
        event.setOutcome(outcome);
        event.setReason(reason == null || reason.isBlank() ? null : reason.trim());
        event.setTraceId(requestContext == null ? null : requestContext.traceId());
        event.setCreatedBy(actorId);
        event.setUpdatedBy(actorId);
        repository.save(event);
    }
}
