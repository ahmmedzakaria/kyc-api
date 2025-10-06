package com.example.kyc.gismodule.controller;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.gismodule.dto.AdministrativeBoundaryResponse;
import com.example.kyc.gismodule.service.implementations.AdministrativeBoundaryService;
import com.example.kyc.kycmodule.dto.KycDto;
import com.example.kyc.kycmodule.dto.KycSearchDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class AdministrativeBoundaryController {

    private final AdministrativeBoundaryService service;

    @Operation(summary = "Search GIS with pagination")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<AdministrativeBoundaryResponse>>> search(@RequestBody KycSearchDto dto) {
        return service.search(dto);
    }
}

