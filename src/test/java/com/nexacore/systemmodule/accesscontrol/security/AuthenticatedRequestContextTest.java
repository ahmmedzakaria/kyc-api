package com.nexacore.systemmodule.accesscontrol.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedRequestContextTest {

    @AfterEach
    void clearHolder() {
        AuthenticatedRequestContextHolder.clear();
    }

    @Test
    void snapshotsEffectivePrivilegesAndExposesTrustedScope() {
        Set<String> privileges = new HashSet<>(Set.of("01010200101"));
        Set<UserScopeAssignment> scopes = new HashSet<>(Set.of(
                new UserScopeAssignment(11L, 12L, 13L),
                new UserScopeAssignment(11L, 12L, 14L)
        ));
        AuthenticatedRequestContext context = new AuthenticatedRequestContext(
                7L, "operator", 3L, "WEB",
                scopes, "trace-1", privileges
        );
        privileges.add("11010100180");
        scopes.add(new UserScopeAssignment(99L, null, null));
        AuthenticatedRequestContextHolder.set(context);

        AuthenticatedRequestContext stored = AuthenticatedRequestContextHolder.get().orElseThrow();
        assertThat(stored.userId()).isEqualTo(7L);
        assertThat(stored.scopeAssignments()).containsExactlyInAnyOrder(
                new UserScopeAssignment(11L, 12L, 13L),
                new UserScopeAssignment(11L, 12L, 14L));
        assertThat(stored.effectivePrivilegeCodes()).containsExactly("01010200101");
        assertThatThrownBy(() -> stored.effectivePrivilegeCodes().add("11010100180"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> stored.scopeAssignments().add(new UserScopeAssignment(99L, null, null)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBranchAssignmentWithoutBusiness() {
        assertThatThrownBy(() -> new UserScopeAssignment(11L, null, 13L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("businessId");
    }

    @Test
    void writableScopeCannotEscalateBranchAssignmentToTenantWideOwnership() {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                7L, "operator", 3L, "WEB",
                Set.of(new UserScopeAssignment(11L, 12L, 13L)), "trace-1", Set.of()
        ));
        DataScopeService service = new DataScopeService();

        assertThat(service.requireWritableScope(null, null, null))
                .isEqualTo(new UserScopeAssignment(11L, 12L, 13L));
        assertThatThrownBy(() -> service.requireWritableScope(11L, null, null))
                .isInstanceOf(DataScopeAccessDeniedException.class);
        assertThat(service.requireWritableScope(11L, 12L, 13L))
                .isEqualTo(new UserScopeAssignment(11L, 12L, 13L));
    }

    @Test
    void tenantWideAssignmentCanCreateWithinDescendantScope() {
        AuthenticatedRequestContextHolder.set(new AuthenticatedRequestContext(
                7L, "manager", 3L, "WEB",
                Set.of(new UserScopeAssignment(11L, null, null)), "trace-1", Set.of()
        ));

        assertThat(new DataScopeService().requireWritableScope(11L, 12L, 13L))
                .isEqualTo(new UserScopeAssignment(11L, 12L, 13L));
    }
}
