package com.nexacore.systemmodule.accesscontrol.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientRedirectUriPolicyTest {
    private final ClientRedirectUriPolicy policy = new ClientRedirectUriPolicy(new ClientOriginPolicy());

    @Test
    void returnsOnlyRegisteredUriForRequestOrigin() {
        assertThat(policy.resolveRegisteredUri(
                "https://app.example/sso/callback,https://admin.example/auth/callback",
                "https://admin.example"))
                .isEqualTo("https://admin.example/auth/callback");
    }

    @Test
    void rejectsOriginWithoutRegisteredRedirect() {
        assertThatThrownBy(() -> policy.resolveRegisteredUri(
                "https://app.example/sso/callback", "https://attacker.example"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsQueryAndFragmentInRegisteredRedirect() {
        assertThatThrownBy(() -> policy.normalizeConfiguredUris("https://app.example/callback?next=evil"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
