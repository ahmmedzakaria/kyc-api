package com.nexacore.kycmodule.person.service.implementations;

import com.nexacore.kycmodule.person.api.PersonRegisteredEvent;
import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import com.nexacore.kycmodule.person.dto.PersonDocumentDto;
import com.nexacore.kycmodule.person.dto.PersonDto;
import com.nexacore.kycmodule.person.entity.KycPerson;
import com.nexacore.kycmodule.person.entity.*;
import com.nexacore.kycmodule.person.repository.PersonDetailsRepository;
import com.nexacore.kycmodule.person.repository.PersonDocumentRepository;
import com.nexacore.kycmodule.person.repository.PersonOrganizationMembershipRepository;
import com.nexacore.kycmodule.person.repository.PersonProfileRepository;
import com.nexacore.kycmodule.person.repository.PersonRepository;
import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import com.nexacore.systemmodule.accesscontrol.security.UserScopeAssignment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonDetailsRepository personDetailsRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final PersonProfileRepository personProfileRepository;
    private final PersonOrganizationMembershipRepository membershipRepository;
    private final ModelMapper mapper;
    private final FileManagementService fileManagementService;
    private final ApplicationEventPublisher eventPublisher;
    private final DataScopeService dataScopeService;
    private final GlobalPersonIdentityGateway globalPersonGateway;

    @Transactional(transactionManager = "kycTransactionManager", readOnly = true)
    public PersonDto getProfile(Long profileId) {
        return toDto(ensureProfile(profileId));
    }

    @Transactional(transactionManager = "kycTransactionManager", rollbackFor = IOException.class)
    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        UserScopeAssignment scope = dataScopeService.requireWritableScope(
                dto.getTenantId(), dto.getBusinessId(), dto.getBranchId());
        long actor = currentActor();
        GlobalPersonIdentityDto globalPerson = resolveGlobalPerson(dto, actor);
        KycPersonProfile profile = personProfileRepository.save(KycPersonProfile.builder()
                .personId(globalPerson.personId()).tenantId(scope.tenantId()).businessId(scope.businessId())
                .branchId(scope.branchId()).tenantEmail(trimToNull(dto.getEmail()))
                .tenantMobile(trimToNull(dto.getMobileNumber())).nationalId(trimToNull(dto.getNationalId()))
                .active(true).createdBy(actor).updatedBy(actor).build());
        membershipRepository.save(KycPersonOrganizationMembership.builder()
                .personId(globalPerson.personId()).tenantId(scope.tenantId()).businessId(scope.businessId())
                .branchId(scope.branchId()).membershipType(PersonMembershipType.CUSTOMER)
                .active(true).primaryMembership(false).createdBy(actor).updatedBy(actor).build());
        savePersonDetails(profile, dto);
        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(profile, PersonDocumentType.PROFILE_PHOTO, photo);
        }
        PersonDto result = toDto(profile);
        eventPublisher.publishEvent(new PersonRegisteredEvent(
                globalPerson.personId(),
                dto.getUsername(),
                Instant.now()
        ));
        return result;
    }

    @Transactional(transactionManager = "kycTransactionManager", rollbackFor = IOException.class)
    public PersonDto update(PersonDto dto, MultipartFile photo) throws IOException {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("Person id is required for update");
        }

        KycPersonProfile profile = ensureProfile(dto.getId());
        profile.setTenantEmail(trimToNull(dto.getEmail()));
        profile.setTenantMobile(trimToNull(dto.getMobileNumber()));
        profile.setNationalId(trimToNull(dto.getNationalId()));
        profile.setUpdatedBy(currentActor());
        personProfileRepository.save(profile);
        savePersonDetails(profile, dto);

        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(profile, PersonDocumentType.PROFILE_PHOTO, photo);
        }

        return toDto(profile);
    }

    @Transactional(transactionManager = "kycTransactionManager", readOnly = true)
    public Page<PersonDto> search(String q, Pageable pageable) {
        List<Long> matchingPersonIds = globalPersonGateway.searchPersonIds(q);
        Specification<KycPersonProfile> textSearch = (root, query, cb) -> matchingPersonIds.isEmpty()
                ? cb.disjunction() : root.get("personId").in(matchingPersonIds);
        return personProfileRepository.findAll(
                        dataScopeService.<KycPersonProfile>restrictToCurrentScopes("tenantId", "businessId", "branchId")
                                .and((root, query, cb) -> cb.isTrue(root.get("active")))
                                .and(textSearch), pageable)
                .map(this::toDto);
    }

    public void delete(Long id) {
        KycPersonProfile profile = ensureProfile(id);
        deleteStoredDocuments(personDocumentRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()));
        personDocumentRepository.deleteByProfileId(profile.getId());
        KycPersonDetails details = personDetailsRepository.findByProfileId(profile.getId());
        if (details != null) personDetailsRepository.delete(details);
        personProfileRepository.delete(profile);
    }

    public byte[] getPhoto(Long id) throws IOException {
        KycPersonProfile profile = ensureProfile(id);
        KycPersonDocument document = personDocumentRepository
                .findFirstByProfileIdAndDocumentTypeOrderByCreatedAtDesc(profile.getId(), PersonDocumentType.PROFILE_PHOTO)
                .orElse(null);
        return document == null ? null : fileManagementService.read(document.getStoragePath());
    }

    public String getPhotoContentType(Long id) {
        KycPersonProfile profile = ensureProfile(id);
        return personDocumentRepository
                .findFirstByProfileIdAndDocumentTypeOrderByCreatedAtDesc(profile.getId(), PersonDocumentType.PROFILE_PHOTO)
                .map(KycPersonDocument::getContentType)
                .orElse(null);
    }

    public List<PersonDocumentDto> getDocuments(Long personId) {
        KycPersonProfile profile = ensureProfile(personId);
        return personDocumentRepository.findByProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(this::toDocumentDto)
                .toList();
    }

    public List<PersonDocumentDto> uploadDocuments(Long personId, PersonDocumentType documentType, MultipartFile[] files) throws IOException {
        KycPersonProfile profile = ensureProfile(personId);
        if (files == null || files.length == 0) {
            return List.of();
        }

        if (isSingleDocumentType(documentType)) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    return List.of(toDocumentDto(upsertSingleDocument(profile, documentType, file)));
                }
            }
            return List.of();
        }

        List<PersonDocumentDto> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            uploaded.add(toDocumentDto(createDocument(profile, documentType, file)));
        }
        return uploaded;
    }

    public PersonDocumentDto getDocument(Long personId, Long documentId) {
        return toDocumentDto(getPersonDocument(personId, documentId));
    }

    public byte[] getDocumentContent(Long personId, Long documentId) throws IOException {
        return fileManagementService.read(getPersonDocument(personId, documentId).getStoragePath());
    }

    public String getDocumentContentType(Long personId, Long documentId) {
        return getPersonDocument(personId, documentId).getContentType();
    }

    private KycPersonProfile ensureProfile(Long profileId) {
        Specification<KycPersonProfile> idMatch = (root, query, cb) -> cb.equal(root.get("id"), profileId);
        return personProfileRepository.findOne(
                        dataScopeService.<KycPersonProfile>restrictToCurrentScopes("tenantId", "businessId", "branchId")
                                .and((root, query, cb) -> cb.isTrue(root.get("active")))
                                .and(idMatch))
                .orElseThrow(() -> new EntityNotFoundException("KYC person profile not found: " + profileId));
    }

    private KycPersonDocument getPersonDocument(Long profileId, Long documentId) {
        KycPersonProfile profile = ensureProfile(profileId);
        return personDocumentRepository.findByIdAndProfileIdAndProfileTenantId(
                        documentId, profile.getId(), profile.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    private KycPersonDocument upsertSingleDocument(KycPersonProfile profile, PersonDocumentType documentType, MultipartFile file) throws IOException {
        KycPersonDocument existingDocument = personDocumentRepository
                .findFirstByProfileIdAndDocumentTypeOrderByCreatedAtDesc(profile.getId(), documentType)
                .orElse(null);

        StoredFile storedFile = fileManagementService.store(
                "person",
                "profiles/" + profile.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                existingDocument == null ? null : existingDocument.getStoragePath()
        );

        KycPersonDocument document = existingDocument == null
                ? KycPersonDocument.builder().personId(profile.getPersonId()).profile(profile).documentType(documentType).build()
                : existingDocument;

        document.setStoragePath(storedFile.path());
        document.setOriginalFilename(storedFile.originalFilename());
        document.setContentType(storedFile.contentType());
        document.setFileSize(storedFile.size());
        document = personDocumentRepository.save(document);

        if (documentType == PersonDocumentType.PROFILE_PHOTO) {
            // Photo ownership and URL are profile-scoped; the global Auth person is not mutated.
        }

        return document;
    }

    private KycPersonDocument createDocument(KycPersonProfile profile, PersonDocumentType documentType, MultipartFile file) throws IOException {
        StoredFile storedFile = fileManagementService.store(
                "person",
                "profiles/" + profile.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                null
        );

        return personDocumentRepository.save(
                KycPersonDocument.builder()
                        .personId(profile.getPersonId())
                        .profile(profile)
                        .documentType(documentType)
                        .storagePath(storedFile.path())
                        .originalFilename(storedFile.originalFilename())
                        .contentType(storedFile.contentType())
                        .fileSize(storedFile.size())
                        .build()
        );
    }

    private void deleteStoredDocuments(List<KycPersonDocument> documents) {
        for (KycPersonDocument document : documents) {
            try {
                fileManagementService.delete(document.getStoragePath());
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isSingleDocumentType(PersonDocumentType documentType) {
        return documentType == PersonDocumentType.PROFILE_PHOTO || documentType == PersonDocumentType.NID;
    }

    private PersonDto toDto(KycPersonProfile profile) {
        GlobalPersonIdentityDto person = globalPersonGateway.findById(profile.getPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Global person not found: " + profile.getPersonId()));
        PersonDto dto = new PersonDto();
        dto.setFirstName(person.firstName());
        dto.setLastName(person.lastName());
        dto.setDateOfBirth(person.dateOfBirth());
        dto.setGender(person.gender());
        dto.setEmail(profile.getTenantEmail() == null ? person.primaryEmail() : profile.getTenantEmail());
        dto.setMobileNumber(profile.getTenantMobile() == null ? person.primaryMobile() : profile.getTenantMobile());
        dto.setCanonicalEmail(person.primaryEmail());
        dto.setCanonicalMobileNumber(person.primaryMobile());
        dto.setOrganizationDeclaredEmail(profile.getTenantEmail());
        dto.setOrganizationDeclaredMobileNumber(profile.getTenantMobile());
        dto.setNationalId(profile.getNationalId());
        dto.setEmailVerified(person.emailVerified());
        dto.setMobileVerified(person.mobileVerified());
        dto.setUser(false);
        dto.setId(profile.getId());
        dto.setPersonId(person.personId());
        dto.setTenantId(profile.getTenantId());
        dto.setBusinessId(profile.getBusinessId());
        dto.setBranchId(profile.getBranchId());
        dto.setPhotoUrl(null);
        dto.setBloodGroup(person.bloodGroup());

        KycPersonDetails details = personDetailsRepository.findByProfileId(profile.getId());
        if (details != null) {
            dto.setFatherName(details.getFatherName());
            dto.setFatherMobileNumber(details.getFatherMobileNumber());
            dto.setMotherName(details.getMotherName());
            dto.setMotherMobileNumber(details.getMotherMobileNumber());
            dto.setEmergencyContactPerson(details.getEmergencyContactPerson());
            dto.setEmergencyContactPersonRelation(details.getEmergencyContactPersonRelation());
            dto.setEmergencyContactNumber(details.getEmergencyContactNumber());
            dto.setEducationLevel(details.getEducationLevel());
            dto.setInstitutionName(details.getInstitutionName());
            dto.setPassingYear(details.getPassingYear());
            dto.setCurrentLocationId(details.getCurrentLocationId());
            dto.setCurrentLocationType(details.getCurrentLocationType());
            dto.setCurrentAddress(details.getCurrentAddress());
            dto.setPermanentLocationId(details.getPermanentLocationId());
            dto.setPermanentLocationType(details.getPermanentLocationType());
            dto.setPermanentAddress(details.getPermanentAddress());
        }

        if (person.personId() != null && personDocumentRepository
                .findFirstByProfileIdAndDocumentTypeOrderByCreatedAtDesc(profile.getId(), PersonDocumentType.PROFILE_PHOTO)
                .isPresent()) {
            dto.setPhotoUrl(buildPhotoApiUrl(profile.getId()));
        }
        return dto;
    }

    private void savePersonDetails(KycPersonProfile profile, PersonDto dto) {
        KycPersonDetails details = personDetailsRepository.findByProfileId(profile.getId());
        if (details == null) {
            details = KycPersonDetails.builder().personId(profile.getPersonId()).profile(profile).build();
        }

        details.setPersonId(profile.getPersonId());
        details.setProfile(profile);
        details.setFatherName(dto.getFatherName());
        details.setFatherMobileNumber(dto.getFatherMobileNumber());
        details.setMotherName(dto.getMotherName());
        details.setMotherMobileNumber(dto.getMotherMobileNumber());
        details.setEmergencyContactPerson(dto.getEmergencyContactPerson());
        details.setEmergencyContactPersonRelation(dto.getEmergencyContactPersonRelation());
        details.setEmergencyContactNumber(dto.getEmergencyContactNumber());
        details.setEducationLevel(dto.getEducationLevel());
        details.setInstitutionName(dto.getInstitutionName());
        details.setPassingYear(dto.getPassingYear());
        details.setCurrentLocationId(dto.getCurrentLocationId());
        details.setCurrentLocationType(dto.getCurrentLocationType());
        details.setCurrentAddress(dto.getCurrentAddress());
        details.setPermanentLocationId(dto.getPermanentLocationId());
        details.setPermanentLocationType(dto.getPermanentLocationType());
        details.setPermanentAddress(dto.getPermanentAddress());

        personDetailsRepository.save(details);
    }

    private PersonDocumentDto toDocumentDto(KycPersonDocument document) {
        return PersonDocumentDto.builder()
                .id(document.getId())
                .personId(document.getPersonId())
                .profileId(document.getProfile().getId())
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .downloadUrl("/api/v1/person/" + document.getProfile().getId() + "/documents/" + document.getId() + "/content")
                .createdAt(document.getCreatedAt())
                .build();
    }

    private String buildPhotoApiUrl(Long personId) {
        return "/api/v1/person/" + personId + "/photo";
    }

    private GlobalPersonIdentityDto resolveGlobalPerson(PersonDto dto, long actor) {
        if (dto.getPersonId() != null) {
            return globalPersonGateway.findById(dto.getPersonId())
                    .orElseThrow(() -> new IllegalArgumentException("Global person not found: " + dto.getPersonId()));
        }
        var byEmail = globalPersonGateway.findByEmail(dto.getEmail());
        if (byEmail.isPresent()) return byEmail.get();
        var byMobile = globalPersonGateway.findByMobile(dto.getMobileNumber());
        if (byMobile.isPresent()) return byMobile.get();
        return globalPersonGateway.create(identityFromDto(dto, null), actor);
    }

    private GlobalPersonIdentityDto identityFromDto(PersonDto dto, Long personId) {
        return GlobalPersonIdentityDto.builder().personId(personId)
                .firstName(dto.getFirstName()).lastName(dto.getLastName())
                .dateOfBirth(dto.getDateOfBirth()).gender(dto.getGender())
                .bloodGroup(dto.getBloodGroup()).primaryEmail(dto.getEmail())
                .primaryMobile(dto.getMobileNumber())
                .emailVerified(Boolean.TRUE.equals(dto.getEmailVerified()))
                .mobileVerified(Boolean.TRUE.equals(dto.getMobileVerified())).active(true).build();
    }

    private long currentActor() {
        return com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder.get()
                .map(context -> context.userId() == null ? 0L : context.userId())
                .orElse(0L);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
