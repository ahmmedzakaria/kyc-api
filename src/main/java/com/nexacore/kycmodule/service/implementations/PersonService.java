package com.nexacore.kycmodule.service.implementations;

import com.nexacore.kycmodule.dto.PersonDocumentDto;
import com.nexacore.kycmodule.dto.PersonDto;
import com.nexacore.kycmodule.entity.Person;
import com.nexacore.kycmodule.entity.PersonDetails;
import com.nexacore.kycmodule.entity.PersonDocument;
import com.nexacore.kycmodule.entity.PersonDocumentType;
import com.nexacore.kycmodule.repository.PersonDetailsRepository;
import com.nexacore.kycmodule.repository.PersonDocumentRepository;
import com.nexacore.kycmodule.repository.PersonRepository;
import com.nexacore.servicesmodule.fileservice.dto.StoredFile;
import com.nexacore.servicesmodule.fileservice.service.interfaces.FileManagementService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonDetailsRepository personDetailsRepository;
    private final PersonDocumentRepository personDocumentRepository;
    private final ModelMapper mapper;
    private final FileManagementService fileManagementService;

    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        Person person = mapPerson(dto, new Person());
        Person savedPerson = personRepository.save(person);
        savePersonDetails(savedPerson, dto);
        if (photo != null && !photo.isEmpty()) {
            upsertSingleDocument(savedPerson, PersonDocumentType.PROFILE_PHOTO, photo);
        }
        return toDto(savedPerson);
    }

    public PersonDto update(PersonDto dto, MultipartFile photo) throws IOException {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("Person id is required for update");
        }

        Person existing = ensurePerson(dto.getId());
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
        Person existing = ensurePerson(id);
        deleteStoredDocuments(personDocumentRepository.findByPersonIdOrderByCreatedAtDesc(id));
        personDocumentRepository.deleteByPerson(existing);
        personRepository.delete(existing);
    }

    public byte[] getPhoto(Long id) throws IOException {
        PersonDocument document = personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(id, PersonDocumentType.PROFILE_PHOTO)
                .orElse(null);
        return document == null ? null : fileManagementService.read(document.getStoragePath());
    }

    public String getPhotoContentType(Long id) {
        return personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(id, PersonDocumentType.PROFILE_PHOTO)
                .map(PersonDocument::getContentType)
                .orElse(null);
    }

    public List<PersonDocumentDto> getDocuments(Long personId) {
        ensurePerson(personId);
        return personDocumentRepository.findByPersonIdOrderByCreatedAtDesc(personId).stream()
                .map(this::toDocumentDto)
                .toList();
    }

    public List<PersonDocumentDto> uploadDocuments(Long personId, PersonDocumentType documentType, MultipartFile[] files) throws IOException {
        Person person = ensurePerson(personId);
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

    private Person ensurePerson(Long personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + personId));
    }

    private PersonDocument getPersonDocument(Long personId, Long documentId) {
        ensurePerson(personId);
        return personDocumentRepository.findById(documentId)
                .filter(document -> document.getPerson().getId().equals(personId))
                .orElseThrow(() -> new EntityNotFoundException("Document not found: " + documentId));
    }

    private PersonDocument upsertSingleDocument(Person person, PersonDocumentType documentType, MultipartFile file) throws IOException {
        PersonDocument existingDocument = personDocumentRepository
                .findFirstByPersonIdAndDocumentTypeOrderByCreatedAtDesc(person.getId(), documentType)
                .orElse(null);

        StoredFile storedFile = fileManagementService.store(
                "person",
                person.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                existingDocument == null ? null : existingDocument.getStoragePath()
        );

        PersonDocument document = existingDocument == null
                ? PersonDocument.builder().person(person).documentType(documentType).build()
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

    private PersonDocument createDocument(Person person, PersonDocumentType documentType, MultipartFile file) throws IOException {
        StoredFile storedFile = fileManagementService.store(
                "person",
                person.getId() + "/" + documentType.name().toLowerCase().replace('_', '-'),
                file,
                null
        );

        return personDocumentRepository.save(
                PersonDocument.builder()
                        .person(person)
                        .documentType(documentType)
                        .storagePath(storedFile.path())
                        .originalFilename(storedFile.originalFilename())
                        .contentType(storedFile.contentType())
                        .fileSize(storedFile.size())
                        .build()
        );
    }

    private void deleteStoredDocuments(List<PersonDocument> documents) {
        for (PersonDocument document : documents) {
            try {
                fileManagementService.delete(document.getStoragePath());
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isSingleDocumentType(PersonDocumentType documentType) {
        return documentType == PersonDocumentType.PROFILE_PHOTO || documentType == PersonDocumentType.NID;
    }

    private PersonDto toDto(Person person) {
        PersonDto dto = mapper.map(person, PersonDto.class);
        dto.setBloodGroup(person.getBloodGrop());

        PersonDetails details = personDetailsRepository.findByPersonId(person.getId());
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

    private Person mapPerson(PersonDto dto, Person person) {
        mapper.map(dto, person);
        person.setBloodGrop(dto.getBloodGroup());
        person.setUsername(generateUniqueUsername(dto.getFirstName(), dto.getLastName(), person.getId()));
        return person;
    }

    private String generateUniqueUsername(String firstName, String lastName, Long currentPersonId) {
        String baseUsername = ((safeName(firstName) + "." + safeName(lastName)).replaceAll("^\\.+|\\.+$", ""))
                .replaceAll("\\.+", ".");
        if (baseUsername.isBlank()) {
            baseUsername = "user";
        }

        String candidate = baseUsername;
        int suffix = 1;
        while (usernameExists(candidate, currentPersonId)) {
            candidate = baseUsername + suffix++;
        }
        return candidate;
    }

    private boolean usernameExists(String username, Long currentPersonId) {
        return currentPersonId == null
                ? personRepository.existsByUsername(username)
                : personRepository.existsByUsernameAndIdNot(username, currentPersonId);
    }

    private String safeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".");
    }

    private void savePersonDetails(Person person, PersonDto dto) {
        PersonDetails details = personDetailsRepository.findByPersonId(person.getId());
        if (details == null) {
            details = PersonDetails.builder().person(person).build();
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

    private PersonDocumentDto toDocumentDto(PersonDocument document) {
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
