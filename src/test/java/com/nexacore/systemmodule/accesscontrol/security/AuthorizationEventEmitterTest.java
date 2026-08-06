package com.nexacore.systemmodule.accesscontrol.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivApiRegistry;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationEventEmitterTest {
    @Test
    void publishesSanitizedStructuredDecisionWithScopeAndDuration() {
        List<Object> published = new ArrayList<>();
        AuthorizationEventEmitter emitter = new AuthorizationEventEmitter(published::add, new ObjectMapper());
        ClientApplicationContext context = ClientApplicationContext.builder()
                .traceId("trace-1")
                .clientApplication(SysPrivClientApplication.builder().clientCode("PARTNER").build())
                .apiRegistry(SysPrivApiRegistry.builder().apiCode("GET:/people/{id}")
                        .pathPattern("/people/{id}").build())
                .requiredPrivilegeCode("01010200101")
                .clientDecision("ALLOWED").userDecision("DENIED")
                .userDenyReason("USER_PRIVILEGE_NOT_ALLOWED")
                .userId(42L)
                .scopeAssignments(Set.of(new UserScopeAssignment(1L, 2L, 3L)))
                .build();

        emitter.emit("GET", context, 2_500_000L);

        assertThat(published).hasSize(1);
        AuthorizationDecisionEvent event = (AuthorizationDecisionEvent) published.getFirst();
        assertThat(event.eventType()).isEqualTo("AUTHORIZATION_DECISION");
        assertThat(event.decision()).isEqualTo("DENIED");
        assertThat(event.denialCode()).isEqualTo("USER_PRIVILEGE_NOT_ALLOWED");
        assertThat(event.userId()).isEqualTo(42L);
        assertThat(event.tenantIds()).containsExactly(1L);
        assertThat(event.businessIds()).containsExactly(2L);
        assertThat(event.branchIds()).containsExactly(3L);
        assertThat(event.durationMicros()).isEqualTo(2500L);
        assertThat(event.toString()).doesNotContain("apiKey", "token", "username");
    }
}
