package com.nexacore.catalogmodule.service;

import com.nexacore.catalogmodule.config.CatalogProperties;
import com.nexacore.gatewaymodule.layout.dto.PublicLayoutContextDto;
import com.nexacore.gatewaymodule.layout.service.interfaces.LayoutModuleGateway;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.nexacore.catalogmodule.repository.PublicCatalogSiteRepository;
import com.nexacore.catalogmodule.api.PublicSiteResponse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import java.util.Optional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogContextServiceTest {
    private final PublicCatalogSiteRepository repository = mock(PublicCatalogSiteRepository.class);
    private final LayoutModuleGateway layoutGateway = mock(LayoutModuleGateway.class);
    private final CatalogContextService service = new CatalogContextService(
            new CatalogProperties("en", List.of("en", "bn"), new CatalogProperties.Media("catalog", "/api/v1/public/media")),
            repository, layoutGateway);

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
                        new PublicSiteResponse.SeoView("Acme", "", "/", logo, false), null)));
        var layout = new PublicLayoutContextDto("WEB_CATALOG_LAYOUT", List.of(), List.of(), null, null);
        when(layoutGateway.getPublicLayout("CATALOG", "https://catalog.acme.test")).thenReturn(layout);
        var response = service.publicContext("en", "https://catalog.acme.test");

        assertThat(response.siteKey()).isEqualTo("acme");
        assertThat(response.hostname()).isEqualTo("catalog.acme.test");
        assertThat(response.supportedLocales()).containsExactly("en", "bn");
        assertThat(response.layout()).isSameAs(layout);
        assertThat(response.toString()).doesNotContain("42");
        verify(layoutGateway).getPublicLayout("CATALOG", "https://catalog.acme.test");
    }

    @Test
    void failsClosedWithoutTrustedHostnameResolution() {
        assertThatThrownBy(() -> service.publicContext("en", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
