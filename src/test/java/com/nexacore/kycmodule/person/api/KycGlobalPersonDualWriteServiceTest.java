package com.nexacore.kycmodule.person.api;

import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import com.nexacore.kycmodule.person.entity.KycPerson;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KycGlobalPersonDualWriteServiceTest {

    @Test
    void mapsCanonicalKycFieldsAndActorToAuthGateway() {
        RecordingGateway gateway = new RecordingGateway();
        KycGlobalPersonDualWriteService service = new KycGlobalPersonDualWriteService(gateway);
        KycPerson person = KycPerson.builder()
                .id(100L)
                .firstName("Rahim")
                .lastName("Ahmed")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .bloodGrop("B+")
                .email("rahim@example.com")
                .mobileNumber("01700000000")
                .emailVerified(true)
                .mobileVerified(false)
                .build();

        service.synchronize(person, 6L);

        assertThat(gateway.actorId).isEqualTo(6L);
        assertThat(gateway.person.personId()).isEqualTo(100L);
        assertThat(gateway.person.firstName()).isEqualTo("Rahim");
        assertThat(gateway.person.bloodGroup()).isEqualTo("B+");
        assertThat(gateway.person.primaryEmail()).isEqualTo("rahim@example.com");
        assertThat(gateway.person.emailVerified()).isTrue();
        assertThat(gateway.person.active()).isTrue();
    }

    private static final class RecordingGateway implements GlobalPersonIdentityGateway {
        private GlobalPersonIdentityDto person;
        private Long actorId;

        @Override
        public GlobalPersonIdentityDto synchronizeFromKyc(GlobalPersonIdentityDto person, Long actorId) {
            this.person = person;
            this.actorId = actorId;
            return person;
        }

        @Override public GlobalPersonIdentityDto create(GlobalPersonIdentityDto person, Long actorId) { return person; }
        @Override public GlobalPersonIdentityDto update(Long id, GlobalPersonIdentityDto person, Long actorId) { return person; }
        @Override public Optional<GlobalPersonIdentityDto> findById(Long id) { return Optional.ofNullable(person); }
        @Override public Optional<GlobalPersonIdentityDto> findByEmail(String email) { return Optional.empty(); }
        @Override public Optional<GlobalPersonIdentityDto> findByMobile(String mobile) { return Optional.empty(); }
        @Override public List<Long> searchPersonIds(String query) { return List.of(); }

        @Override
        public boolean existsById(Long personId) {
            return person != null && person.personId().equals(personId);
        }
    }
}
