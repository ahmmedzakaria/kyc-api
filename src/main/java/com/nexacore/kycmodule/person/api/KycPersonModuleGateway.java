package com.nexacore.kycmodule.person.api;

import com.nexacore.kycmodule.person.entity.KycPerson;
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
@Transactional(transactionManager = "kycTransactionManager", readOnly = true)
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
    @Transactional(transactionManager = "kycTransactionManager")
    public PersonSummaryDto ensurePersonForUser(String username,
                                                String email,
                                                String mobileNumber,
                                                String firstName,
                                                String lastName) {
        Optional<KycPerson> existingPerson = findExistingPerson(username, email, mobileNumber);
        KycPerson person = existingPerson.orElseGet(KycPerson::new);

        if (!StringUtils.hasText(person.getUsername())) {
            person.setUsername(username.trim());
        }
        if (!StringUtils.hasText(person.getEmail()) && StringUtils.hasText(email)) {
            person.setEmail(email.trim());
        }
        if (!StringUtils.hasText(person.getMobileNumber()) && StringUtils.hasText(mobileNumber)) {
            person.setMobileNumber(mobileNumber.trim());
        }
        if (!StringUtils.hasText(person.getFirstName()) && StringUtils.hasText(firstName)) {
            person.setFirstName(firstName.trim());
        }
        if (!StringUtils.hasText(person.getLastName()) && StringUtils.hasText(lastName)) {
            person.setLastName(lastName.trim());
        }
        if (person.getEmailVerified() == null) {
            person.setEmailVerified(false);
        }
        if (person.getMobileVerified() == null) {
            person.setMobileVerified(false);
        }

        return toSummary(personRepository.save(person));
    }

    @Override
    public boolean existsById(Long personId) {
        return personId != null && personRepository.existsById(personId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return StringUtils.hasText(username) && personRepository.existsByUsername(username.trim());
    }

    private Optional<KycPerson> findExistingPerson(String username, String email, String mobileNumber) {
        if (StringUtils.hasText(username)) {
            Optional<KycPerson> person = personRepository.findByUsername(username.trim());
            if (person.isPresent()) {
                return person;
            }
        }
        if (StringUtils.hasText(email)) {
            Optional<KycPerson> person = personRepository.findByEmail(email.trim());
            if (person.isPresent()) {
                return person;
            }
        }
        if (StringUtils.hasText(mobileNumber)) {
            return personRepository.findByMobileNumber(mobileNumber.trim());
        }
        return Optional.empty();
    }

    private PersonSummaryDto toSummary(KycPerson person) {
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
