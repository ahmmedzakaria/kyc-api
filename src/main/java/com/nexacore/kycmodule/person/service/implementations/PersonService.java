package com.nexacore.kycmodule.person.service.implementations;

import com.nexacore.kycmodule.person.api.PersonRegisteredEvent;
import com.nexacore.kycmodule.person.api.KycGlobalPersonDualWriteService;
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

    private final PersonRepository personRepository;
    private final PersonDetailsRepository personDetailsRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final PersonProfileRepository personProfileRepository;
    private final PersonOrganizationMembershipRepository membershipRepository;
    private final ModelMapper mapper;
    private final FileManagementService fileManagementService;
    private final ApplicationEventPublisher eventPublisher;
    private final DataScopeService dataScopeService;
    private final KycGlobalPersonDualWriteService dualWriteService;

    @Transactional(transactionManager = "kycTransactionManager", rollbackFor = IOException.class)
    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        UserScopeAssignment scope = dataScopeService.requireWritableScope(
                dto.getTenantId(), dto.getBusinessId(), dto.getBranchId());
        KycPerson person = resolveGlobalPerson(dto);
        mapPerson(dto, person);
        KycPerson savedPerson = personRepository.save(person);
        long actor = currentActor();
        KycPersonProfile profile = personProfileRepository.save(KycPersonProfile.builder()
                .person(savedPerson).tenantId(scope.tenantId()).businessId(scope.businessId())
                .branchId(scope.branchId()).active(true).createdBy(actor).updatedBy(actor).build());
        membershipRepository.save(KycPersonOrganizationMembership.builder()
                .person(savedPerson).tenantId(scope.tenantId()).businessId(scope.businessId())
                .branchId(scope.branchId()).membershipType(PersonMembershipType.CUSTOMER)
                .active(true).primaryMembership(false).createdBy(actor).updatedBy(actor).build());
        savePersonDetails(profile, dto);
        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(profile, PersonDocumentType.PROFILE_PHOTO, photo);
        }
        dualWriteService.synchronize(savedPerson, actor);
        PersonDto result = toDto(profile);
        eventPublisher.publishEvent(new PersonRegisteredEvent(
                savedPerson.getId(),
                savedPerson.getUsername(),
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
        KycPerson existing = profile.getPerson();
        mapPerson(dto, existing);
        personRepository.save(existing);
        savePersonDetails(profile, dto);

        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(profile, PersonDocumentType.PROFILE_PHOTO, photo);
        }

        dualWriteService.synchronize(existing, currentActor());

        return toDto(profile);
    }

    @Transactional(transactionManager = "kycTransactionManager", readOnly = true)
    public Page<PersonDto> search(String q, Pageable pageable) {
        String searchText = q == null ? "" : q.trim().toLowerCase();
        Specification<KycPersonProfile> textSearch = (root, query, cb) -> {
            String pattern = "%" + searchText + "%";
            var person = root.get("person");
            return cb.or(
                    cb.like(cb.lower(cb.coalesce(person.get("firstName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(person.get("lastName"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(person.get("email"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(person.get("mobileNumber"), "")), pattern)
            );
        };
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
        return personDocumentRepository.findById(documentId)
                .filter(document -> document.getProfile().getId().equals(profile.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    private KycPersonDocument upsertSingleDocument(KycPersonProfile profile, PersonDocumentType documentType, MultipartFile file) throws IOException {
        KycPerson person = profile.getPerson();
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
                ? KycPersonDocument.builder().person(person).profile(profile).documentType(documentType).build()
                : existingDocument;

        document.setStoragePath(storedFile.path());
        document.setOriginalFilename(storedFile.originalFilename());
        document.setContentType(storedFile.contentType());
        document.setFileSize(storedFile.size());
        document = personDocumentRepository.save(document);

        if (documentType == PersonDocumentType.PROFILE_PHOTO) {
            person.setPhotoUrl(buildPhotoApiUrl(person.getId()));
            personRepository.save(person);
        }

        return document;
    }

    private KycPersonDocument createDocument(KycPersonProfile profile, PersonDocumentType documentType, MultipartFile file) throws IOException {
        KycPerson person = profile.getPerson();
        StoredFile storedFile = fileManagementService.store(
                "person",
                "profiles/" + profile.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                null
        );

        return personDocumentRepository.save(
                KycPersonDocument.builder()
                        .person(person)
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
        KycPerson person = profile.getPerson();
        PersonDto dto = mapper.map(person, PersonDto.class);
        dto.setId(profile.getId());
        dto.setPersonId(person.getId());
        dto.setTenantId(profile.getTenantId());
        dto.setBusinessId(profile.getBusinessId());
        dto.setBranchId(profile.getBranchId());
        dto.setPhotoUrl(null);
        dto.setBloodGroup(person.getBloodGrop());

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

        if (person.getId() != null && personDocumentRepository
                .findFirstByProfileIdAndDocumentTypeOrderByCreatedAtDesc(profile.getId(), PersonDocumentType.PROFILE_PHOTO)
                .isPresent()) {
            dto.setPhotoUrl(buildPhotoApiUrl(profile.getId()));
        }
        return dto;
    }

    private KycPerson mapPerson(PersonDto dto, KycPerson person) {
        String authUsername = person.getUsername();
        Boolean isUser = person.getUser();
        mapper.map(dto, person);
        person.setBloodGrop(dto.getBloodGroup());
        person.setUsername(authUsername);
        person.setUser(isUser == null ? false : isUser);
        return person;
    }

    private void savePersonDetails(KycPersonProfile profile, PersonDto dto) {
        KycPerson person = profile.getPerson();
        KycPersonDetails details = personDetailsRepository.findByProfileId(profile.getId());
        if (details == null) {
            details = KycPersonDetails.builder().person(person).profile(profile).build();
        }

        details.setPerson(person);
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
                .personId(document.getPerson().getId())
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

    private KycPerson resolveGlobalPerson(PersonDto dto) {
        if (dto.getEmail() != null) {
            var existing = personRepository.findByEmail(dto.getEmail().trim());
            if (existing.isPresent()) return existing.get();
        }
        if (dto.getMobileNumber() != null) {
            var existing = personRepository.findByMobileNumber(dto.getMobileNumber().trim());
            if (existing.isPresent()) return existing.get();
        }
        return new KycPerson();
    }

    private long currentActor() {
        return com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextHolder.get()
                .map(context -> context.userId() == null ? 0L : context.userId())
                .orElse(0L);
    }
}
