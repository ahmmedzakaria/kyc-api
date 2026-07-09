package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.dto.ClientApplicationContextDto;

import java.util.Optional;
import java.util.Set;

public interface ClientApplicationContextService {
    Optional<ClientApplicationContextDto> getCurrentClientContext();

    Set<String> getCurrentClientPrivilegeCodes(Set<String> userPrivilegeCodes);

    boolean hasCurrentClient();
}
