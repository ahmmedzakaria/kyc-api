package com.example.kyc.kycmodule.service.implementations;

import com.example.kyc.commonmodule.dto.ApiResponse;
import com.example.kyc.kycmodule.dto.KycDto;
import com.example.kyc.commonmodule.dto.SearchDto;
import com.example.kyc.kycmodule.entity.KycRecord;
import com.example.kyc.kycmodule.repository.KycRecordRepository;
import com.example.kyc.kycmodule.service.interfaces.KycService;
import com.example.kyc.commonmodule.service.interfaces.StorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycServiceImpl implements KycService {

    private final KycRecordRepository repo;
    private final StorageService storageService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<KycDto>> create(KycDto dto, MultipartFile photo) throws Exception {
        try {
            log.info("Creating KYC for nationalId={}", dto.getNationalId());
//        if (repo.existsByNationalId(dto.getNationalId())) {
//            throw new IllegalArgumentException("nationalId already exists");
//        }
            KycRecord r = KycRecord.builder()
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    //.nationalId(dto.getNationalId())
                    .email(dto.getEmail())
                    .phone(dto.getPhone())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            if (photo != null && !photo.isEmpty()) {
                String path = storageService.store(photo, null);
                r.setPhotoPath(path);
                r.setPhotoContentType(photo.getContentType());
            }
            KycRecord saved = repo.save(r);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(toDto(saved), "Person Information Saved"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<KycDto>> update(KycDto dto, MultipartFile photo) {
        try {
            log.info("Updating KYC id={}", dto.getId());
            KycRecord r = repo.findById(dto.getId()).orElseThrow(() -> new IllegalArgumentException("not found"));
            if (dto.getFirstName() != null) r.setFirstName(dto.getFirstName());
            if (dto.getLastName() != null) r.setLastName(dto.getLastName());
            if (dto.getEmail() != null) r.setEmail(dto.getEmail());
            if (dto.getPhone() != null) r.setPhone(dto.getPhone());
//        if (dto.getNationalId() != null && !dto.getNationalId().equals(r.getNationalId())) {
//            if (repo.existsByNationalId(dto.getNationalId())) throw new IllegalArgumentException("nationalId already exists");
//            r.setNationalId(dto.getNationalId());
//        }
            if (photo != null && !photo.isEmpty()) {
                String path = storageService.store(photo, r.getPhotoPath());
                r.setPhotoPath(path);
                r.setPhotoContentType(photo.getContentType());
            }
            r.setUpdatedAt(Instant.now());
            KycRecord saved = repo.save(r);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(toDto(saved), "Person Information Updated"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> delete(Long id)  {
        try {
            log.info("Deleting KYC id={}", id);
            KycRecord r = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
            if (r.getPhotoPath() != null) {
                try {
                    storageService.delete(r.getPhotoPath());
                } catch (Exception e) {
                    log.warn("failed to delete photo: {}", e.getMessage());
                }
            }
            repo.deleteById(id);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Person Information Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<KycDto>> getById(Long id) {
        try {
            KycDto dto = repo.findById(id).map(this::toDto).orElseThrow(() -> new IllegalArgumentException("not found"));
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dto, "Person Information Fetched Successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<Page<KycDto>>> search(SearchDto searchDto) {
        try {
            Page<KycRecord> pageData = null;
            Pageable pageable = PageRequest.of(searchDto.page(), searchDto.size(), Sort.by(Sort.Direction.DESC, (searchDto.sort() == null || searchDto.sort().isBlank()) ? "id" : searchDto.sort()));
//        if (nationalId != null && !nationalId.isBlank()) {
//            //return repo.findByNationalId(nationalId, pageable).map(this::toDto);
//        }
            if (searchDto.searchText() == null || searchDto.searchText().isBlank()) {
                pageData = repo.findAll(pageable);
            } else {
                pageData = repo.findByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContaining(searchDto.searchText(), searchDto.searchText(), pageable);
            }

            Page<KycDto> response = pageData.map(p -> {
                KycDto dto = toDto(p);
                try {
                    byte[] photo = getPhoto(p); // load bytes from DB, FS, or S3
                    if (photo != null) {
                        dto.setPhotoString(Base64.getEncoder().encodeToString(photo));
                    }
                } catch (Exception e) {
                    log.warn("photo not found for id {}: {}", p.getId(), e.getMessage());
                }
                return dto;
            });
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response, "Search Completed Successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }

    }

    @Override
    public byte[] getPhoto(Long id) throws Exception {
        KycRecord r = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("not found"));
        if (r.getPhotoPath() == null) return null;
        return storageService.read(r.getPhotoPath());
    }

    public byte[] getPhoto(KycRecord r) throws Exception {
        if (r.getPhotoPath() == null) return null;
        return storageService.read(r.getPhotoPath());
    }

    @Override
    public String getPhotoContentType(Long id) {
        return repo.findById(id).map(KycRecord::getPhotoContentType).orElse(null);
    }

    private KycDto toDto(KycRecord r) {
        return KycDto.builder()
                .id(r.getId())
                .firstName(r.getFirstName())
                .lastName(r.getLastName())
                // .nationalId(r.getNationalId())
                .email(r.getEmail())
                .phone(r.getPhone())
                .build();
    }
}
