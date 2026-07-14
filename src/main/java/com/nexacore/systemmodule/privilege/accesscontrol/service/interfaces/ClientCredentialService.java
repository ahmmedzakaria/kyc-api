package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysClientApplication;

import java.util.Optional;

public interface ClientCredentialService {
    Optional<SysClientApplication> validateApiKey(String clientCode, String apiKey);
}
