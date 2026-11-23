package com.nexacore.catalogmodule.controller;

import com.nexacore.catalogmodule.api.AdminCatalogContextResponse;
import com.nexacore.catalogmodule.service.CatalogContextService;
import com.nexacore.systemmodule.accesscontrol.security.ApiDataScope;
import com.nexacore.systemmodule.accesscontrol.security.PrivilegeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/catalog/admin")
public class AdminCatalogController {
    public static final String VIEW_PRIVILEGE = "12010100101";

    private final CatalogContextService contextService;

    @GetMapping("/context")
    @PrivilegeApi(value = VIEW_PRIVILEGE, dataScope = ApiDataScope.TENANT)
    public ResponseEntity<AdminCatalogContextResponse> context() {
        return ResponseEntity.ok(contextService.adminContext());
    }
}
