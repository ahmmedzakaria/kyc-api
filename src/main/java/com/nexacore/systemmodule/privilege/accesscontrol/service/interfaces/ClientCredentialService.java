package com.nexacore.systemmodule.privilege.accesscontrol.service.interfaces;

import com.nexacore.systemmodule.privilege.accesscontrol.entity.SysPrivClientApplication;

import java.util.Optional;

public interface ClientCredentialService {
    Optional<SysPrivClientApplication> validateApiKey(String clientCode, String apiKey);
}
