package com.nexacore.systemmodule.clientaccess.service.interfaces;

import com.nexacore.systemmodule.clientaccess.entity.SysClientApplication;

import java.util.Optional;

public interface ClientCredentialService {
    Optional<SysClientApplication> validateApiKey(String clientCode, String apiKey);
}
