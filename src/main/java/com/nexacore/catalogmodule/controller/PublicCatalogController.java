package com.nexacore.catalogmodule.controller;

import com.nexacore.catalogmodule.api.PublicSiteResponse;
import com.nexacore.catalogmodule.service.CatalogContextService;
import com.nexacore.systemmodule.accesscontrol.security.PublicApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class PublicCatalogController {
    private final CatalogContextService contextService;

    @GetMapping("/site")
    @PublicApi
    public ResponseEntity<PublicSiteResponse> site(
            @RequestParam(value = "lang", defaultValue = "en") String locale) {
        return ResponseEntity.ok(contextService.publicContext(locale));
    }
}
