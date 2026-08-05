package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationContextDto;

import java.util.Optional;
import java.util.Set;

public interface ClientApplicationContextService {
    Optional<ClientApplicationContextDto> getCurrentClientContext();

    Set<String> getCurrentClientPrivilegeCodes(Set<String> userPrivilegeCodes);

    boolean hasCurrentClient();
}
