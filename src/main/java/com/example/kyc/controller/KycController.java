package com.example.kyc.controller;

import com.example.kyc.dto.KycDto;
import com.example.kyc.dto.documentation.KycCreateRequest;
import com.example.kyc.service.KycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/kyc")
@Tag(name = "KYC API", description = "Operations for managing KYC records")
@RequiredArgsConstructor
@Slf4j
public class KycController {

    private final KycService service;

//    @Operation(summary = "Create a KYC record")
    @Operation(
            summary = "Create KYC with JSON data and optional photo",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = KycCreateRequest.class)
                    )
            )
    )
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycDto> create(@ModelAttribute KycDto dto,
                                         @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        KycDto created = service.create(dto, photo);
        return ResponseEntity.created(URI.create("/api/kyc/" + created.getId())).body(created);
    }

//    public ResponseEntity<KycDto> create(@RequestPart("data") @Valid KycDto dto,
//                                         @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
//        KycDto created = service.create(dto, photo);
//        return ResponseEntity.created(URI.create("/api/kyc/" + created.getId())).body(created);
//    }

    @Operation(summary = "Update a KYC record")
    @PostMapping(path = "update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<KycDto> update(@ModelAttribute KycDto dto,
                                         @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        KycDto updated = service.update( dto, photo);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete a KYC record")
    @PostMapping("delete/{id}")
    public ResponseEntity<KycDto> delete(@PathVariable("id") Long id) throws Exception {
        service.delete(id);
        return ResponseEntity.ok(new KycDto());
//        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get KYC by Id")
    @GetMapping("{id}")
    public ResponseEntity<KycDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Operation(summary = "Search KYC with pagination")
    @GetMapping("/search")
    public ResponseEntity<Page<KycDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nationalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(service.search(q, nationalId, page, size, sort));
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
