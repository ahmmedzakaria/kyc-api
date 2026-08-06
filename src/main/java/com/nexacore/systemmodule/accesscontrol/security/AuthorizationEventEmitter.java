package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationEventEmitter {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    public void emit(String httpMethod, ClientApplicationContext context, long durationNanos) {
        if (context == null) return;
        SysPrivApiRegistry api = context.apiRegistry();
        Set<UserScopeAssignment> scopes = context.scopeAssignments() == null ? Set.of() : context.scopeAssignments();
        AuthorizationDecisionEvent event = new AuthorizationDecisionEvent(
                null, context.traceId(), httpMethod,
                api == null ? null : api.getApiCode(), api == null ? null : api.getPathPattern(),
                context.clientApplication() == null ? null : context.clientApplication().getClientCode(),
                context.userId(), context.decision() == null ? "ALLOWED" : context.decision(),
                context.denyReason(), context.requiredPrivilegeCode(),
                scopes.stream().map(UserScopeAssignment::tenantId).collect(Collectors.toUnmodifiableSet()),
                scopes.stream().map(UserScopeAssignment::businessId).filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet()),
                scopes.stream().map(UserScopeAssignment::branchId).filter(java.util.Objects::nonNull).collect(Collectors.toUnmodifiableSet()),
                durationNanos / 1_000
        );
        try {
            applicationEventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.warn("Failed to publish authorization event traceId={}", context.traceId());
        }
        try {
            log.info("authorization_event={}", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize authorization event traceId={}", context.traceId());
        }
    }
}
