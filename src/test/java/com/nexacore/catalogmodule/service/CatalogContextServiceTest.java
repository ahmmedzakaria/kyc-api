package com.nexacore.catalogmodule.service;

import com.nexacore.catalogmodule.config.CatalogProperties;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.nexacore.catalogmodule.repository.PublicCatalogSiteRepository;
import com.nexacore.catalogmodule.api.PublicSiteResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Optional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogContextServiceTest {
    private final PublicCatalogSiteRepository repository = mock(PublicCatalogSiteRepository.class);
    private final CatalogContextService service = new CatalogContextService(
            new CatalogProperties("en", List.of("en", "bn"), new CatalogProperties.Media("catalog", "/api/v1/public/media")), repository);

    @AfterEach
    void clearTenant() {
        ResolvedTenantContextHolder.clear();
    }

    @Test
    void exposesPresentationSafePublicContextForResolvedTenant() {
        ResolvedTenantContextHolder.set(new ResolvedTenantContext(42L, "acme", "catalog.acme.test"));

        var logo = new PublicSiteResponse.MediaView("logo", "IMAGE", "/logo.svg", "Acme", null, null, null, null);
        when(repository.findPublished(42L, "catalog.acme.test", "en", List.of("en", "bn"), "/api/v1/public/media"))
                .thenReturn(Optional.of(new PublicSiteResponse("acme", "catalog.acme.test", "Acme", logo,
                        List.of("en", "bn"), "en", List.of(), List.of(),
                        new PublicSiteResponse.ContactView("", "", "", null), List.of(),
                        new PublicSiteResponse.SeoView("Acme", "", "/", logo, false))));
        var response = service.publicContext("en");

        assertThat(response.siteKey()).isEqualTo("acme");
        assertThat(response.hostname()).isEqualTo("catalog.acme.test");
        assertThat(response.supportedLocales()).containsExactly("en", "bn");
        assertThat(response.toString()).doesNotContain("42");
    }

    @Test
    void failsClosedWithoutTrustedHostnameResolution() {
        assertThatThrownBy(() -> service.publicContext("en"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
