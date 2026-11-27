package com.nexacore.catalogmodule.service;

import com.nexacore.catalogmodule.api.AdminCatalogContextResponse;
import com.nexacore.catalogmodule.api.PublicSiteResponse;
import com.nexacore.catalogmodule.config.CatalogProperties;
import com.nexacore.catalogmodule.repository.PublicCatalogSiteRepository;
import com.nexacore.gatewaymodule.layout.service.interfaces.LayoutModuleGateway;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContext;
import com.nexacore.systemmodule.tenant.security.ResolvedTenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CatalogContextService {
    private static final String CATALOG_CLIENT_CODE = "CATALOG";

    private final CatalogProperties properties;
    private final PublicCatalogSiteRepository siteRepository;
    private final LayoutModuleGateway layoutModuleGateway;

    public PublicSiteResponse publicContext(String locale, String origin) {
        ResolvedTenantContext tenant = requiredTenant();
        String selectedLocale = properties.supportedLocales().contains(locale) ? locale : properties.defaultLocale();
        PublicSiteResponse site = siteRepository.findPublished(tenant.tenantId(), tenant.hostname(), selectedLocale,
                        properties.supportedLocales(), properties.media().publicPath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No published catalog site for this hostname"));
        return site.withLayout(layoutModuleGateway.getPublicLayout(CATALOG_CLIENT_CODE, origin));
    }

    public AdminCatalogContextResponse adminContext() {
        ResolvedTenantContext tenant = requiredTenant();
        return new AdminCatalogContextResponse(
                tenant.tenantId(), tenant.tenantCode(), tenant.hostname(), "ROLE_CONTROLLED_DRAFT_PUBLISH");
    }

    private ResolvedTenantContext requiredTenant() {
        return ResolvedTenantContextHolder.get().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No active catalog site for this hostname"));
    }
}
