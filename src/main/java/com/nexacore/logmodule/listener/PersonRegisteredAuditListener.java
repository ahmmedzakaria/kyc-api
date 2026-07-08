package com.nexacore.logmodule.listener;

import com.nexacore.kycmodule.person.api.PersonRegisteredEvent;
import com.nexacore.logmodule.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PersonRegisteredAuditListener {

    private final LogService logService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PersonRegisteredEvent event) {
        logService.writeAuditLog(
                event.username(),
                "PERSON_REGISTERED",
                "Person",
                String.valueOf(event.personId()),
                "Person registered at " + event.occurredAt()
        );
    }
}
