package com.nexacore.catalogmodule.api;

/** Administrative contract. This must never be returned by a public endpoint. */
public record AdminCatalogContextResponse(
        long tenantId,
        String tenantCode,
        String hostname,
        String publicationWorkflow
) {
}
