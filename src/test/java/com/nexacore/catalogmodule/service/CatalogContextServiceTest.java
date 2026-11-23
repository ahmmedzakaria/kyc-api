package com.nexacore.catalogmodule.service;

import com.nexacore.catalogmodule.config.CatalogProperties;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogContextServiceTest {
    private final CatalogContextService service = new CatalogContextService(
            new CatalogProperties("en", List.of("en", "bn"),
                    new CatalogProperties.Media("catalog", "/api/v1/public/media")));

    @AfterEach
    void clearTenant() {
        ResolvedTenantContextHolder.clear();
    }

    @Test
    void exposesPresentationSafePublicContextForResolvedTenant() {
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(42L, "acme", "catalog.acme.test"));

        var response = service.publicContext();

        assertThat(response.siteKey()).isEqualTo("acme");
        assertThat(response.hostname()).isEqualTo("catalog.acme.test");
        assertThat(response.supportedLocales()).containsExactly("en", "bn");
        assertThat(response.toString()).doesNotContain("42");
    }

    @Test
    void failsClosedWithoutTrustedHostnameResolution() {
        assertThatThrownBy(service::publicContext)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
