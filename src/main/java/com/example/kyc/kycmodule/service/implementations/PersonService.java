package com.example.kyc.kycmodule.service.implementations;


import com.example.kyc.kycmodule.dto.PersonDto;
import com.example.kyc.kycmodule.entity.Person;
import com.example.kyc.kycmodule.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final ModelMapper mapper;
    private final Path uploadDir = Paths.get("./uploads/persons").toAbsolutePath();

    public PersonDto create(PersonDto dto, MultipartFile photo) throws IOException {
        if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

        Person person = mapper.map(dto, Person.class);
        if (photo != null && !photo.isEmpty()) {
            String filename = dto.getUsername() + "_" + photo.getOriginalFilename();
            Path path = uploadDir.resolve(filename);
            Files.copy(photo.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            person.setPhotoUrl("/uploads/persons/" + filename);
        }
        return mapper.map(personRepository.save(person), PersonDto.class);
    }

    public Page<PersonDto> search(String q, Pageable pageable) {
        return personRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q, pageable)
                .map(p -> mapper.map(p, PersonDto.class));
    }

    public void delete(Long id) {
        personRepository.deleteById(id);
    }
}

