package com.nexacore.systemmodule.privilege.security;

import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("privilegeAuthorizer")
@RequiredArgsConstructor
public class PrivilegeAuthorizer {

    private final PrivilegeModuleGateway privilegeModuleGateway;

    public boolean has(Authentication authentication, String privilegeCode) {
        return authentication != null
                && authentication.isAuthenticated()
                && privilegeCode != null
                && !privilegeCode.isBlank()
                && privilegeModuleGateway.hasPrivilege(authentication.getName(), privilegeCode);
    }
}
