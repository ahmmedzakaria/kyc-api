package com.example.kyc.kycmodule.service.implementations;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.kyc.kycmodule.dto.PersonDto;
import com.example.kyc.kycmodule.entity.Person;
import com.example.kyc.kycmodule.repository.PersonRepository;
import com.example.kyc.servicesmodule.fileservice.dto.StoredFile;
import com.example.kyc.servicesmodule.fileservice.service.interfaces.FileManagementService;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final ModelMapper mapper;
    private final FileManagementService fileManagementService;

    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        Person person = mapper.map(dto, Person.class);
        if (photo != null && !photo.isEmpty()) {
            StoredFile storedFile = fileManagementService.store("person", "profile-photo", photo, null);
            person.setPhotoUrl(storedFile.publicUrl());
        }
        return mapper.map(personRepository.save(person), PersonDto.class);
    }

    public PersonDto update(PersonDto dto, MultipartFile photo) throws IOException {
        if (dto.getId() == null) {
            throw new IllegalArgumentException("Person id is required for update");
        }

        Person existing = personRepository.findById(dto.getId())
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + dto.getId()));

        mapper.map(dto, existing);

        if (photo != null && !photo.isEmpty()) {
            StoredFile storedFile = fileManagementService.store("person", "profile-photo", photo, existing.getPhotoUrl());
            existing.setPhotoUrl(storedFile.publicUrl());
        }

        return mapper.map(personRepository.save(existing), PersonDto.class);
    }

    public Page<PersonDto> search(String q, Pageable pageable) {
        return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q, pageable)
                .map(p -> {
                    PersonDto dto = mapper.map(p, PersonDto.class);
                    if (p.getPhotoUrl() != null && !p.getPhotoUrl().isBlank()) {
                        dto.setPhotoUrl("/api/v1/person/" + p.getId() + "/photo");
                    }
                    return dto;
                });
    }

    public void delete(Long id) {
        Person existing = personRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + id));
        if (existing.getPhotoUrl() != null) {
            try {
                fileManagementService.delete(existing.getPhotoUrl());
            } catch (Exception ignored) {
            }
        }
        personRepository.delete(existing);
    }

    public byte[] getPhoto(Long id) throws IOException {
        Person existing = personRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + id));
        if (existing.getPhotoUrl() == null || existing.getPhotoUrl().isBlank()) {
            return null;
        }
        return fileManagementService.read(existing.getPhotoUrl());
    }

    public String getPhotoContentType(Long id) {
        return personRepository.findById(id)
                .map(Person::getPhotoUrl)
                .filter(url -> !url.isBlank())
                .map(fileManagementService::contentType)
                .orElse(null);
    }
}
