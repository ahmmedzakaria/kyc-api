package com.nexacore.kycmodule.person.backfill;

import com.nexacore.gatewaymodule.identity.service.interfaces.GlobalPersonIdentityGateway;
import com.nexacore.kycmodule.person.api.KycGlobalPersonDualWriteService;
import com.nexacore.kycmodule.person.entity.KycPerson;
import com.nexacore.kycmodule.person.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalPersonBackfillService {
    private final PersonRepository personRepository;
    private final GlobalPersonIdentityGateway identityGateway;
    private final KycGlobalPersonDualWriteService dualWriteService;

    public GlobalPersonBackfillReport backfill(int requestedPageSize) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 1_000));
        long examined = 0;
        long created = 0;
        long updated = 0;
        List<Long> failedIds = new ArrayList<>();
        int pageNumber = 0;
        Page<KycPerson> page;

        do {
            page = personRepository.findAll(PageRequest.of(
                    pageNumber++, pageSize, Sort.by(Sort.Direction.ASC, "id")));
            for (KycPerson person : page.getContent()) {
                examined++;
                try {
                    boolean exists = identityGateway.existsById(person.getId());
                    dualWriteService.synchronize(person, 0L);
                    if (exists) updated++; else created++;
                } catch (RuntimeException exception) {
                    failedIds.add(person.getId());
                    log.error("Global person backfill failed for KYC person {}", person.getId(), exception);
                }
            }
        } while (page.hasNext());

        GlobalPersonBackfillReport report = new GlobalPersonBackfillReport(
                examined, created, updated, failedIds.size(), List.copyOf(failedIds));
        log.info("Global person backfill completed: examined={}, created={}, updated={}, failed={}, failedPersonIds={}",
                report.examined(), report.created(), report.updated(), report.failed(), report.failedPersonIds());
        return report;
    }
}
