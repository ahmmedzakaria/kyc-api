package com.example.kyc.kycmodule.controller;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.commonmodule.dto.FileRequestDto;
import com.example.kyc.commonmodule.dto.IdRequestDto;
import com.example.kyc.commonmodule.dto.SearchDto;
import com.example.kyc.kycmodule.dto.PersonDocumentDto;
import com.example.kyc.kycmodule.dto.PersonDto;
import com.example.kyc.kycmodule.entity.PersonDocumentType;
import com.example.kyc.kycmodule.service.implementations.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/person")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService service;

    @Operation(summary = "Create a new person")
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PersonDto>> create(
            @ModelAttribute PersonDto dto,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(service.create(dto, photo),"Created person"));
    }

    @Operation(summary = "Update an existing person")
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PersonDto>> update(
            @ModelAttribute PersonDto dto,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws Exception {
        return ResponseEntity.ok(ApiResponse.success(service.update(dto, photo), "Updated person"));
    }

    @Operation(summary = "Search persons (paginated)")
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<PersonDto>>> search(@RequestBody SearchDto dto) {
        Pageable pageable = PageRequest.of(dto.page(), dto.size(), Sort.by("firstName"));
        String searchText = dto.searchText() == null ? "" : dto.searchText();
        return ResponseEntity.ok(ApiResponse.success(service.search(searchText, pageable),""));
    }

    @Operation(summary = "Delete person by ID")
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestBody IdRequestDto idRequestDto) {
        service.delete(Long.valueOf(idRequestDto.getId()));
        return ResponseEntity.ok(ApiResponse.success(null, "Person deleted"));
    }

    @Operation(summary = "Get person photo")
    @PostMapping("/photo")
    public ResponseEntity<byte[]> photo(@RequestBody FileRequestDto requestDto) throws Exception {
        byte[] data = service.getPhoto(requestDto.getOwnerId());
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = service.getPhotoContentType(requestDto.getOwnerId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                .body(data);
    }

    @Operation(summary = "Upload person documents")
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PersonDocumentDto>>> uploadDocuments(
            @ModelAttribute FileRequestDto requestDto,
            @RequestParam("files") MultipartFile[] files) throws Exception {
        PersonDocumentType documentType = PersonDocumentType.valueOf(requestDto.getFileType());
        return ResponseEntity.ok(ApiResponse.success(service.uploadDocuments(requestDto.getOwnerId(), documentType, files), "Person documents uploaded"));
    }

    @Operation(summary = "List person documents")
    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<List<PersonDocumentDto>>> documents(@RequestBody FileRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(service.getDocuments(requestDto.getOwnerId()), "Person documents fetched"));
    }

    @Operation(summary = "Get person document metadata")
    @PostMapping("/document")
    public ResponseEntity<ApiResponse<PersonDocumentDto>> document(@RequestBody FileRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.success(service.getDocument(requestDto.getOwnerId(), requestDto.getFileId()), "Person document fetched"));
    }

    @Operation(summary = "Download person document content")
    @PostMapping("/document/content")
    public ResponseEntity<byte[]> documentContent(@RequestBody FileRequestDto requestDto) throws Exception {
        byte[] data = service.getDocumentContent(requestDto.getOwnerId(), requestDto.getFileId());
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = service.getDocumentContentType(requestDto.getOwnerId(), requestDto.getFileId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType)
                .body(data);
    }
}
