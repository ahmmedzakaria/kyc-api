package com.nexacore.kycmodule.person.service.implementations;

import com.nexacore.kycmodule.person.api.PersonRegisteredEvent;
import com.nexacore.kycmodule.person.dto.PersonDocumentDto;
import com.nexacore.kycmodule.person.dto.PersonDto;
import com.nexacore.kycmodule.person.entity.KycPerson;
import com.nexacore.kycmodule.person.entity.KycPersonDetails;
import com.nexacore.kycmodule.person.entity.KycPersonDocument;
import com.nexacore.kycmodule.person.entity.PersonDocumentType;
import com.nexacore.kycmodule.person.repository.PersonDetailsRepository;
import com.nexacore.kycmodule.person.repository.PersonDocumentRepository;
import com.nexacore.kycmodule.person.repository.PersonRepository;
import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ModelMapper mapper;
    private final FileManagementService fileManagementService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(transactionManager = "kycTransactionManager", rollbackFor = IOException.class)
    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        KycPerson person = mapPerson(dto, new KycPerson());
        KycPerson savedPerson = personRepository.save(person);
        savePersonDetails(savedPerson, dto);
        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(savedPerson, PersonDocumentType.PROFILE_PHOTO, photo);
        }
        PersonDto result = toDto(savedPerson);
        eventPublisher.publishEvent(new PersonRegisteredEvent(
                savedPerson.getId(),
                savedPerson.getUsername(),
                Instant.now()
        ));
        return result;
    }

    public PersonDto update(PersonDto dto, MultipartFile photo) throws IOException {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("Person id is required for update");
        }

        KycPerson existing = ensurePerson(dto.getId());
        mapPerson(dto, existing);
        personRepository.save(existing);
        savePersonDetails(existing, dto);

        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(existing, PersonDocumentType.PROFILE_PHOTO, photo);
        }

        return toDto(existing);
    }

    public Page<PersonDto> search(String q, Pageable pageable) {
        return personRepository.searchByNameEmailOrMobile(q == null ? "" : q.trim(), pageable)
                .map(this::toDto);
    }

    public void delete(Long id) {
        KycPerson existing = ensurePerson(id);
        deleteStoredDocuments(personDocumentRepository.findByPersonIdOrderByCreatedAtDesc(id));
        personDocumentRepository.deleteByPerson(existing);
        personRepository.delete(existing);
    }

    public byte[] getPhoto(Long id) throws IOException {
        KycPersonDocument document = personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(id, PersonDocumentType.PROFILE_PHOTO)
                .orElse(null);
        return document == null ? null : fileManagementService.read(document.getStoragePath());
    }

    public String getPhotoContentType(Long id) {
        return personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(id, PersonDocumentType.PROFILE_PHOTO)
                .map(KycPersonDocument::getContentType)
                .orElse(null);
    }

    public List<PersonDocumentDto> getDocuments(Long personId) {
        ensurePerson(personId);
        return personDocumentRepository.findByPersonIdOrderByCreatedAtDesc(personId).stream()
                .map(this::toDocumentDto)
                .toList();
    }

    public List<PersonDocumentDto> uploadDocuments(Long personId, PersonDocumentType documentType, MultipartFile[] files) throws IOException {
        KycPerson person = ensurePerson(personId);
        if (files == null || files.length == 0) {
            return List.of();
        }

        if (isSingleDocumentType(documentType)) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    return List.of(toDocumentDto(upsertSingleDocument(person, documentType, file)));
                }
            }
            return List.of();
        }

        List<PersonDocumentDto> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            uploaded.add(toDocumentDto(createDocument(person, documentType, file)));
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

    private KycPerson ensurePerson(Long personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + personId));
    }

    private KycPersonDocument getPersonDocument(Long personId, Long documentId) {
        ensurePerson(personId);
        return personDocumentRepository.findById(documentId)
                .filter(document -> document.getPerson().getId().equals(personId))
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    private KycPersonDocument upsertSingleDocument(KycPerson person, PersonDocumentType documentType, MultipartFile file) throws IOException {
        KycPersonDocument existingDocument = personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(person.getId(), documentType)
                .orElse(null);

        StoredFile storedFile = fileManagementService.store(
                "person",
                person.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                existingDocument == null ? null : existingDocument.getStoragePath()
        );

        KycPersonDocument document = existingDocument == null
                ? KycPersonDocument.builder().person(person).documentType(documentType).build()
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

    private KycPersonDocument createDocument(KycPerson person, PersonDocumentType documentType, MultipartFile file) throws IOException {
        StoredFile storedFile = fileManagementService.store(
                "person",
                person.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                null
        );

        return personDocumentRepository.save(
                KycPersonDocument.builder()
                        .person(person)
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

    private PersonDto toDto(KycPerson person) {
        PersonDto dto = mapper.map(person, PersonDto.class);
        dto.setBloodGroup(person.getBloodGrop());

        KycPersonDetails details = personDetailsRepository.findByPersonId(person.getId());
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
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(person.getId(), PersonDocumentType.PROFILE_PHOTO)
                .isPresent()) {
            dto.setPhotoUrl(buildPhotoApiUrl(person.getId()));
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

    private void savePersonDetails(KycPerson person, PersonDto dto) {
        KycPersonDetails details = personDetailsRepository.findByPersonId(person.getId());
        if (details == null) {
            details = KycPersonDetails.builder().person(person).build();
        }

        details.setPerson(person);
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
                .documentType(document.getDocumentType())
                .originalFilename(document.getOriginalFilename())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .downloadUrl("/api/v1/person/" + document.getPerson().getId() + "/documents/" + document.getId() + "/content")
                .createdAt(document.getCreatedAt())
                .build();
    }

    private String buildPhotoApiUrl(Long personId) {
        return "/api/v1/person/" + personId + "/photo";
    }
}
