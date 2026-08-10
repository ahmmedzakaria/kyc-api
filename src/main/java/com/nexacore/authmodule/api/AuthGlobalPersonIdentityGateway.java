package com.nexacore.authmodule.api;

import com.nexacore.authmodule.core.entity.AuthPerson;
import com.nexacore.authmodule.core.enums.AuthPersonStatus;
import com.nexacore.authmodule.core.repository.AuthPersonRepository;
import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(transactionManager = "authTransactionManager")
public class AuthGlobalPersonIdentityGateway implements GlobalPersonIdentityGateway {
    private final AuthPersonRepository repository;

    @PersistenceContext(unitName = "auth")
    private EntityManager entityManager;

    @Override
    public GlobalPersonIdentityDto synchronizeFromKyc(GlobalPersonIdentityDto source, Long actorId) {
        if (source == null || source.personId() == null) {
            throw new IllegalArgumentException("Global person ID is required");
        }
        if (source.firstName() == null || source.firstName().isBlank()) {
            throw new IllegalArgumentException("Global person first name is required");
        }

        long actor = actorId == null ? 0L : actorId;
        boolean created = !repository.existsById(source.personId());
        AuthPerson person = repository.findById(source.personId()).orElseGet(() -> {
            AuthPerson value = new AuthPerson();
            value.setId(source.personId());
            value.setCreatedBy(actor);
            return value;
        });
        person.setFirstName(trimRequired(source.firstName()));
        person.setMiddleName(trimToNull(source.middleName()));
        person.setLastName(trimToNull(source.lastName()));
        person.setDateOfBirth(source.dateOfBirth());
        person.setGender(trimToNull(source.gender()));
        person.setBloodGroup(trimToNull(source.bloodGroup()));
        person.setPrimaryEmail(trimToNull(source.primaryEmail()));
        person.setPrimaryMobile(trimToNull(source.primaryMobile()));
        person.setEmailVerified(source.emailVerified());
        person.setMobileVerified(source.mobileVerified());
        person.setStatus(source.active() ? AuthPersonStatus.ACTIVE : AuthPersonStatus.INACTIVE);
        person.setActive(source.active());
        person.setUpdatedBy(actor);
        AuthPerson saved = repository.saveAndFlush(person);

        if (created) {
            entityManager.createNativeQuery("""
                    SELECT setval(
                        pg_get_serial_sequence('auth_persons', 'id'),
                        GREATEST(
                            (SELECT COALESCE(MAX(id), 1) FROM auth_persons),
                            (SELECT last_value FROM auth_persons_id_seq)
                        ),
                        true
                    )
                    """).getSingleResult();
        }
        return toDto(saved);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public boolean existsById(Long personId) {
        return personId != null && repository.existsById(personId);
    }

    private String trimRequired(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) throw new IllegalArgumentException("Global person first name is required");
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private GlobalPersonIdentityDto toDto(AuthPerson person) {
        return GlobalPersonIdentityDto.builder()
                .personId(person.getId())
                .firstName(person.getFirstName())
                .middleName(person.getMiddleName())
                .lastName(person.getLastName())
                .dateOfBirth(person.getDateOfBirth())
                .gender(person.getGender())
                .bloodGroup(person.getBloodGroup())
                .primaryEmail(person.getPrimaryEmail())
                .primaryMobile(person.getPrimaryMobile())
                .emailVerified(person.isEmailVerified())
                .mobileVerified(person.isMobileVerified())
                .active(person.isActive())
                .build();
    }
}
