package com.nexacore.gismodule.controller;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.commonmodule.dto.IdRequestDto;
import com.nexacore.gismodule.dto.AdministrativeBoundaryResponse;
import com.nexacore.gismodule.service.implementations.AdministrativeBoundaryService;
import com.nexacore.commonmodule.dto.SearchDto;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class AdministrativeBoundaryController {

    private final AdministrativeBoundaryService service;

    @Operation(summary = "Search GIS with pagination")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<AdministrativeBoundaryResponse>>> search(@RequestBody SearchDto dto) {
        ApiResponse<Page<AdministrativeBoundaryResponse>> data = service.search(dto);
        return data.toResponseEntity();
    }

    @Operation(summary = "Get GIS location by id and gis code")
    @PostMapping("/get-by-id")
    public ResponseEntity<ApiResponse<AdministrativeBoundaryResponse>> getById(@RequestBody IdRequestDto dto) {
        ApiResponse<AdministrativeBoundaryResponse> data = service.getById(UUID.fromString(dto.getId()), dto.getType());
        return data.toResponseEntity();
    }
}
