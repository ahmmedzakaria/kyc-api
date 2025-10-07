package com.example.kyc.gismodule.controller;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.gismodule.dto.AdministrativeBoundaryResponse;
import com.example.kyc.gismodule.service.implementations.AdministrativeBoundaryService;
import com.example.kyc.commonmodule.dto.SearchDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class AdministrativeBoundaryController {

    private final AdministrativeBoundaryService service;

    @Operation(summary = "Search GIS with pagination")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<AdministrativeBoundaryResponse>>> search(@RequestBody SearchDto dto) {
        return service.search(dto);
    }
}

