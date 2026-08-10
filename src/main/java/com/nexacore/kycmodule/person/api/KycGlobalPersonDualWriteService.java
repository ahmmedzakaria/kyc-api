package com.nexacore.kycmodule.person.api;

import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import com.nexacore.kycmodule.person.entity.KycPerson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KycGlobalPersonDualWriteService {
    private final GlobalPersonIdentityGateway globalPersonIdentityGateway;

    public GlobalPersonIdentityDto synchronize(KycPerson person, Long actorId) {
        if (person == null || person.getId() == null) {
            throw new IllegalArgumentException("Persisted KYC person is required for Auth synchronization");
        }
        return globalPersonIdentityGateway.synchronizeFromKyc(
                GlobalPersonIdentityDto.builder()
                        .personId(person.getId())
                        .firstName(person.getFirstName())
                        .lastName(person.getLastName())
                        .dateOfBirth(person.getDateOfBirth())
                        .gender(person.getGender())
                        .bloodGroup(person.getBloodGrop())
                        .primaryEmail(person.getEmail())
                        .primaryMobile(person.getMobileNumber())
                        .emailVerified(Boolean.TRUE.equals(person.getEmailVerified()))
                        .mobileVerified(Boolean.TRUE.equals(person.getMobileVerified()))
                        .active(true)
                        .build(),
                actorId == null ? 0L : actorId
        );
    }
}
