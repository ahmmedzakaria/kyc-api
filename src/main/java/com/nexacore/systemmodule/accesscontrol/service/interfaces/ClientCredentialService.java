package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;

import java.util.Optional;

public interface ClientCredentialService {
    Optional<SysAccClientApplication> resolveActiveClient(String clientCode);

    Optional<SysAccClientApplication> validateApiKey(String clientCode, String apiKey);
}
