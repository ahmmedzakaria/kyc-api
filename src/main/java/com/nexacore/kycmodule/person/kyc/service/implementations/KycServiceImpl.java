package com.nexacore.kycmodule.person.kyc.service.implementations;

import com.nexacore.commonmodule.dto.ApiResponse;
import com.nexacore.kycmodule.person.kyc.dto.KycDto;
import com.nexacore.commonmodule.dto.SearchDto;
import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import com.nexacore.kycmodule.person.kyc.entity.KycRecord;
import com.nexacore.kycmodule.person.kyc.repository.KycRecordRepository;
import com.nexacore.kycmodule.person.kyc.service.interfaces.KycService;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder;
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
    private final FileManagementService fileManagementService;
    private final DataScopeService dataScopeService;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<KycDto>> create(KycDto dto, MultipartFile photo) throws Exception {
        try {
            log.info("Creating KYC for nationalId={}", dto.getNationalId());
            long tenantId = dataScopeService.requireEffectiveTenant(null);
            long actorId = currentActor();
//        if (repo.existsByNationalId(dto.getNationalId())) {
//            throw new IllegalArgumentException("nationalId already exists");
//        }
            KycRecord r = KycRecord.builder()
                    .tenantId(tenantId)
                    .firstName(dto.getFirstName())
                    .lastName(dto.getLastName())
                    //.nationalId(dto.getNationalId())
                    .email(dto.getEmail())
                    .phone(dto.getPhone())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .createdBy(actorId)
                    .updatedBy(actorId)
                    .build();
            if (photo != null && !photo.isEmpty()) {
                StoredFile storedFile = fileManagementService.store("kyc", "profile-photo", photo, null);
                r.setPhotoPath(storedFile.path());
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
            KycRecord r = findOwned(dto.getId());
            if (dto.getFirstName() != null) r.setFirstName(dto.getFirstName());
            if (dto.getLastName() != null) r.setLastName(dto.getLastName());
            if (dto.getEmail() != null) r.setEmail(dto.getEmail());
            if (dto.getPhone() != null) r.setPhone(dto.getPhone());
//        if (dto.getNationalId() != null && !dto.getNationalId().equals(r.getNationalId())) {
//            if (repo.existsByNationalId(dto.getNationalId())) throw new IllegalArgumentException("nationalId already exists");
//            r.setNationalId(dto.getNationalId());
//        }
            if (photo != null && !photo.isEmpty()) {
                StoredFile storedFile = fileManagementService.store("kyc", "profile-photo", photo, r.getPhotoPath());
                r.setPhotoPath(storedFile.path());
                r.setPhotoContentType(photo.getContentType());
            }
            r.setUpdatedAt(Instant.now());
            r.setUpdatedBy(currentActor());
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
            KycRecord r = findOwned(id);
            if (r.getPhotoPath() != null) {
                try {
                    fileManagementService.delete(r.getPhotoPath());
                } catch (Exception e) {
                    log.warn("failed to delete photo: {}", e.getMessage());
                }
            }
            repo.delete(r);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Person Information Deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), List.of("Exception occurs: " + e.getLocalizedMessage())));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<KycDto>> getById(Long id) {
        try {
            KycDto dto = toDto(findOwned(id));
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
                pageData = repo.findAll((root, query, cb) -> cb.equal(root.get("tenantId"),
                        dataScopeService.requireEffectiveTenant(null)), pageable);
            } else {
                long tenantId = dataScopeService.requireEffectiveTenant(null);
                String text = "%" + searchDto.searchText().toLowerCase() + "%";
                pageData = repo.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.or(cb.like(cb.lower(root.get("firstName")), text),
                                cb.like(cb.lower(root.get("lastName")), text))), pageable);
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
        KycRecord r = findOwned(id);
        if (r.getPhotoPath() == null) return null;
        return fileManagementService.read(r.getPhotoPath());
    }

    public byte[] getPhoto(KycRecord r) throws Exception {
        if (r.getPhotoPath() == null) return null;
        return fileManagementService.read(r.getPhotoPath());
    }

    @Override
    public String getPhotoContentType(Long id) {
        return findOwned(id).getPhotoContentType();
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

    private KycRecord findOwned(Long id) {
        long tenantId = dataScopeService.requireEffectiveTenant(null);
        return repo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("not found"));
    }

    private long currentActor() {
        return AuthenticatedRequestContextHolder.get().map(context -> context.userId() == null ? 0L : context.userId()).orElse(0L);
    }
}
