package com.nexacore.kycmodule.person.api;

import com.nexacore.kycmodule.person.entity.Person;
import com.nexacore.kycmodule.person.repository.PersonRepository;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KycPersonModuleGateway implements PersonModuleGateway {

    private final PersonRepository personRepository;

    @Override
    public Optional<PersonSummaryDto> findSummaryById(Long personId) {
        if (personId == null) {
            return Optional.empty();
        }
        return personRepository.findById(personId).map(this::toSummary);
    }

    @Override
    public Optional<PersonSummaryDto> findSummaryByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return personRepository.findByUsername(username.trim()).map(this::toSummary);
    }

    @Override
    public Optional<PersonSummaryDto> findSummaryByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return personRepository.findByEmail(email.trim()).map(this::toSummary);
    }

    @Override
    public Optional<PersonSummaryDto> findSummaryByMobileNumber(String mobileNumber) {
        if (!StringUtils.hasText(mobileNumber)) {
            return Optional.empty();
        }
        return personRepository.findByMobileNumber(mobileNumber.trim()).map(this::toSummary);
    }

    @Override
    public boolean existsById(Long personId) {
        return personId != null && personRepository.existsById(personId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return StringUtils.hasText(username) && personRepository.existsByUsername(username.trim());
    }

    private PersonSummaryDto toSummary(Person person) {
        return PersonSummaryDto.builder()
                .id(person.getId())
                .username(person.getUsername())
                .mobileNumber(person.getMobileNumber())
                .email(person.getEmail())
                .firstName(person.getFirstName())
                .lastName(person.getLastName())
                .dateOfBirth(person.getDateOfBirth())
                .gender(person.getGender())
                .nationalId(person.getNationalId())
                .emailVerified(person.getEmailVerified())
                .mobileVerified(person.getMobileVerified())
                .build();
    }
}
