package com.nexacore.authmodule.api;

import com.nexacore.authmodule.core.entity.AuthPerson;
import com.nexacore.authmodule.core.enums.AuthPersonStatus;
import com.nexacore.authmodule.core.repository.AuthPersonRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Component
@Primary
@RequiredArgsConstructor
@Transactional(transactionManager = "authTransactionManager")
public class AuthPersonModuleGateway implements PersonModuleGateway {
    private final AuthPersonRepository persons;
    private final UserRepository users;

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<PersonSummaryDto> findSummaryById(Long personId) {
        return personId == null ? Optional.empty() : persons.findById(personId).map(this::summary);
    }

    @Override
    public Optional<PersonSummaryDto> findSummaryByUsername(String username) {
        // Username belongs to a tenant account. A global username-only lookup is ambiguous.
        return Optional.empty();
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<PersonSummaryDto> findSummaryByEmail(String email) {
        return !StringUtils.hasText(email) ? Optional.empty()
                : persons.findFirstByPrimaryEmailIgnoreCase(email.trim()).map(this::summary);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<PersonSummaryDto> findSummaryByMobileNumber(String mobileNumber) {
        return !StringUtils.hasText(mobileNumber) ? Optional.empty()
                : persons.findFirstByPrimaryMobile(mobileNumber.trim()).map(this::summary);
    }

    @Override
    public PersonSummaryDto ensurePersonForUser(String username, String email, String mobileNumber,
                                                String firstName, String lastName) {
        AuthPerson person = findExisting(email, mobileNumber).orElseGet(AuthPerson::new);
        applyMissing(person, email, mobileNumber, firstName, lastName);
        return withUsername(summary(persons.saveAndFlush(person)), username);
    }

    @Override
    public PersonSummaryDto promotePersonToUser(Long personId, String username, String email,
                                                String firstName, String lastName) {
        AuthPerson person = persons.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Global person not found: " + personId));
        applyMissing(person, email, null, firstName, lastName);
        return withUsername(summary(persons.saveAndFlush(person)), username);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public boolean existsById(Long personId) {
        return personId != null && persons.existsById(personId);
    }

    @Override
    public boolean existsByUsername(String username) {
        return false;
    }

    private Optional<AuthPerson> findExisting(String email, String mobile) {
        if (StringUtils.hasText(email)) {
            Optional<AuthPerson> person = persons.findFirstByPrimaryEmailIgnoreCase(email.trim());
            if (person.isPresent()) return person;
        }
        return StringUtils.hasText(mobile) ? persons.findFirstByPrimaryMobile(mobile.trim()) : Optional.empty();
    }

    private void applyMissing(AuthPerson person, String email, String mobile, String firstName, String lastName) {
        if (!StringUtils.hasText(person.getFirstName())) {
            if (!StringUtils.hasText(firstName)) throw new IllegalArgumentException("Global person first name is required");
            person.setFirstName(firstName.trim());
        }
        if (!StringUtils.hasText(person.getLastName()) && StringUtils.hasText(lastName)) person.setLastName(lastName.trim());
        if (!StringUtils.hasText(person.getPrimaryEmail()) && StringUtils.hasText(email)) person.setPrimaryEmail(email.trim());
        if (!StringUtils.hasText(person.getPrimaryMobile()) && StringUtils.hasText(mobile)) person.setPrimaryMobile(mobile.trim());
        if (person.getStatus() == null) person.setStatus(AuthPersonStatus.ACTIVE);
        person.setActive(true);
        if (person.getCreatedBy() == null) person.setCreatedBy(0L);
        person.setUpdatedBy(0L);
    }

    private PersonSummaryDto summary(AuthPerson person) {
        return PersonSummaryDto.builder().id(person.getId())
                .mobileNumber(person.getPrimaryMobile()).email(person.getPrimaryEmail())
                .firstName(person.getFirstName()).lastName(person.getLastName())
                .dateOfBirth(person.getDateOfBirth()).gender(person.getGender())
                .emailVerified(person.isEmailVerified()).mobileVerified(person.isMobileVerified())
                .user(person.getId() != null && users.existsByPersonId(person.getId())).build();
    }

    private PersonSummaryDto withUsername(PersonSummaryDto summary, String username) {
        summary.setUsername(username);
        return summary;
    }
}
