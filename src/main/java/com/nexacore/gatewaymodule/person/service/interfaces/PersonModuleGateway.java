package com.nexacore.gatewaymodule.person.service.interfaces;

import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

import java.util.Optional;

public interface PersonModuleGateway extends ModuleGateway {
    Optional<PersonSummaryDto> findSummaryById(Long personId);

    Optional<PersonSummaryDto> findSummaryByUsername(String username);

    Optional<PersonSummaryDto> findSummaryByEmail(String email);

    Optional<PersonSummaryDto> findSummaryByMobileNumber(String mobileNumber);

    PersonSummaryDto ensurePersonForUser(String username,
                                         String email,
                                         String mobileNumber,
                                         String firstName,
                                         String lastName);

    PersonSummaryDto promotePersonToUser(Long personId,
                                         String username,
                                         String email,
                                         String firstName,
                                         String lastName);

    boolean existsById(Long personId);

    boolean existsByUsername(String username);
}
