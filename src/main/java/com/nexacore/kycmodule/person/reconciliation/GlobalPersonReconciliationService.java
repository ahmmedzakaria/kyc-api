package com.nexacore.kycmodule.person.reconciliation;

import com.nexacore.gatewaymodule.identity.dto.GlobalPersonIdentityDto;
import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import com.nexacore.kycmodule.person.entity.KycPerson;
import com.nexacore.kycmodule.person.repository.PersonProfileRepository;
import com.nexacore.kycmodule.person.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GlobalPersonReconciliationService {
    private final PersonProfileRepository profiles;
    private final PersonRepository legacyPersons;
    private final GlobalPersonIdentityGateway authPersons;

    public GlobalPersonReconciliationReport reconcile() {
        List<Long> profileIds = profiles.findDistinctPersonIds();
        List<Long> missing = profileIds.stream().filter(id -> !authPersons.existsById(id)).toList();
        List<Long> divergent = new ArrayList<>();
        long examined = 0;
        for (KycPerson legacy : legacyPersons.findAll()) {
            examined++;
            authPersons.findById(legacy.getId()).ifPresent(auth -> {
                if (diverges(legacy, auth)) divergent.add(legacy.getId());
            });
        }
        return new GlobalPersonReconciliationReport(profileIds.size(), List.copyOf(missing),
                examined, List.copyOf(divergent));
    }

    private boolean diverges(KycPerson legacy, GlobalPersonIdentityDto auth) {
        return !Objects.equals(legacy.getFirstName(), auth.firstName())
                || !Objects.equals(legacy.getLastName(), auth.lastName())
                || !Objects.equals(legacy.getDateOfBirth(), auth.dateOfBirth())
                || !Objects.equals(legacy.getGender(), auth.gender())
                || !Objects.equals(legacy.getEmail(), auth.primaryEmail())
                || !Objects.equals(legacy.getMobileNumber(), auth.primaryMobile());
    }
}
