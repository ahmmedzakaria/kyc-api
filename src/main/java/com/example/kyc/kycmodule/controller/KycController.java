package com.example.kyc.kycmodule.controller;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.commonmodule.dto.IdRequest;
import com.example.kyc.kycmodule.dto.KycDto;
import com.example.kyc.commonmodule.dto.SearchDto;
import com.example.kyc.kycmodule.dto.documentation.KycCreateRequest;
import com.example.kyc.kycmodule.service.interfaces.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/kyc")
@Tag(name = "KYC API", description = "Operations for managing KYC records")
@RequiredArgsConstructor
@Slf4j
public class KycController {

    private final KycService service;

    @Operation(
            summary = "Create KYC with JSON data and optional photo",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = KycCreateRequest.class)
                    )
            )
    )
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycDto>> create(@ModelAttribute KycDto dto,
                                         @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        return service.create(dto, photo);
    }

    @Operation(summary = "Update a KYC record")
    @PostMapping(path = "update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<KycDto>> update(@ModelAttribute KycDto dto,
                                         @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        return service.update( dto, photo);
    }

    @Operation(summary = "Delete a KYC record")
    @PostMapping("delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody IdRequest idRequest) throws Exception {
        return service.delete(idRequest.getId());
    }

    @Operation(summary = "Get KYC by Id")
    @PostMapping("get-by-id")
    public ResponseEntity<ApiResponse<KycDto>> getById(@RequestBody IdRequest idRequest) {
        return service.getById(idRequest.getId());
    }

    @Operation(summary = "Search KYC with pagination")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<KycDto>>> search(@RequestBody SearchDto dto) {

        return service.search(dto);
    }

    @Operation(summary = "Get photo attachment")
    @GetMapping("{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) throws Exception {
        byte[] data = service.getPhoto(id);
        if (data == null) return ResponseEntity.notFound().build();
        String ct = service.getPhotoContentType(id);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, ct == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : ct).body(data);
    }
}
