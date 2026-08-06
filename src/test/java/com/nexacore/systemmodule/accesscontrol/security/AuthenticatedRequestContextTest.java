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
        AuthenticatedRequestContext context = new AuthenticatedRequestContext(
                7L, "operator", 3L, "WEB",
                11L, 12L, 13L, "trace-1", privileges
        );
        privileges.add("11010100180");
        AuthenticatedRequestContextHolder.set(context);

        AuthenticatedRequestContext stored = AuthenticatedRequestContextHolder.get().orElseThrow();
        assertThat(stored.userId()).isEqualTo(7L);
        assertThat(stored.tenantId()).isEqualTo(11L);
        assertThat(stored.businessId()).isEqualTo(12L);
        assertThat(stored.branchId()).isEqualTo(13L);
        assertThat(stored.effectivePrivilegeCodes()).containsExactly("01010200101");
        assertThatThrownBy(() -> stored.effectivePrivilegeCodes().add("11010100180"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
