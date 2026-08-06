package com.nexacore.systemmodule.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;

import java.util.Optional;

public interface ClientCredentialService {
    Optional<SysPrivClientApplication> resolveActiveClient(String clientCode);

    Optional<SysPrivClientApplication> validateApiKey(String clientCode, String apiKey);
}
