package com.example.kyc.kycmodule.controller;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.kycmodule.dto.PersonDto;
import com.example.kyc.kycmodule.service.implementations.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @Operation(summary = "Create a new person")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PersonDto>> create(
            @RequestPart("data") PersonDto dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(service.create(dto, photo),"Created person"));
    }

    @Operation(summary = "Search persons (paginated)")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PersonDto>>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("firstName"));
        return ResponseEntity.ok(ApiResponse.success(service.search(q, pageable),""));
    }

    @Operation(summary = "Delete person by ID")
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Person deleted"));
    }
}

