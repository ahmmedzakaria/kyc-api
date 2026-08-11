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
import java.util.List;
import java.util.Optional;

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
        AuthPerson person = repository.findById(source.personId()).orElse(null);
        if (person == null) {
            insertPreservingId(source, actor);
            advanceIdentitySequence();
            return toDto(repository.findById(source.personId()).orElseThrow());
        }
        apply(source, person, actor);
        return toDto(repository.saveAndFlush(person));
    }

    @Override
    public GlobalPersonIdentityDto create(GlobalPersonIdentityDto source, Long actorId) {
        requireSource(source);
        long actor = actorId == null ? 0L : actorId;
        AuthPerson person = new AuthPerson();
        apply(source, person, actor);
        person.setCreatedBy(actor);
        return toDto(repository.saveAndFlush(person));
    }

    @Override
    public GlobalPersonIdentityDto update(Long personId, GlobalPersonIdentityDto source, Long actorId) {
        requireSource(source);
        AuthPerson person = repository.findById(personId)
                .orElseThrow(() -> new IllegalArgumentException("Global person not found: " + personId));
        apply(source, person, actorId == null ? 0L : actorId);
        return toDto(repository.saveAndFlush(person));
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<GlobalPersonIdentityDto> findById(Long personId) {
        return personId == null ? Optional.empty() : repository.findById(personId).map(this::toDto);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<GlobalPersonIdentityDto> findByEmail(String email) {
        return email == null || email.isBlank() ? Optional.empty()
                : repository.findFirstByPrimaryEmailIgnoreCase(email.trim()).map(this::toDto);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Optional<GlobalPersonIdentityDto> findByMobile(String mobile) {
        return mobile == null || mobile.isBlank() ? Optional.empty()
                : repository.findFirstByPrimaryMobile(mobile.trim()).map(this::toDto);
    }

    @Override
    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public List<Long> searchPersonIds(String query) {
        String text = query == null ? "" : query.trim();
        return repository.searchActiveIds(text);
    }

    private void requireSource(GlobalPersonIdentityDto source) {
        if (source == null || source.firstName() == null || source.firstName().isBlank()) {
            throw new IllegalArgumentException("Global person first name is required");
        }
    }

    private void apply(GlobalPersonIdentityDto source, AuthPerson person, long actor) {
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
    }

    private void insertPreservingId(GlobalPersonIdentityDto source, long actor) {
        entityManager.createNativeQuery("""
                INSERT INTO auth_persons (
                    id, first_name, middle_name, last_name, date_of_birth, gender,
                    blood_group, primary_email, primary_mobile, email_verified,
                    mobile_verified, status, active, created_by, updated_by,
                    created_at, updated_at
                ) VALUES (
                    :id, :firstName, :middleName, :lastName, :dateOfBirth, :gender,
                    :bloodGroup, :primaryEmail, :primaryMobile, :emailVerified,
                    :mobileVerified, :status, :active, :actor, :actor,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """)
                .setParameter("id", source.personId())
                .setParameter("firstName", trimRequired(source.firstName()))
                .setParameter("middleName", trimToNull(source.middleName()))
                .setParameter("lastName", trimToNull(source.lastName()))
                .setParameter("dateOfBirth", source.dateOfBirth())
                .setParameter("gender", trimToNull(source.gender()))
                .setParameter("bloodGroup", trimToNull(source.bloodGroup()))
                .setParameter("primaryEmail", trimToNull(source.primaryEmail()))
                .setParameter("primaryMobile", trimToNull(source.primaryMobile()))
                .setParameter("emailVerified", source.emailVerified())
                .setParameter("mobileVerified", source.mobileVerified())
                .setParameter("status", source.active()
                        ? AuthPersonStatus.ACTIVE.name() : AuthPersonStatus.INACTIVE.name())
                .setParameter("active", source.active())
                .setParameter("actor", actor)
                .executeUpdate();
        entityManager.clear();
    }

    private void advanceIdentitySequence() {
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
