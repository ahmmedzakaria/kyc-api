package com.nexacore.authmodule.privilege.api;

import com.nexacore.gatewaymodule.privilege.service.interfaces.PrivilegeModuleGateway;
import com.nexacore.systemmodule.privilege.service.interfaces.PrivilegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthPrivilegeModuleGateway implements PrivilegeModuleGateway {

    private final PrivilegeService privilegeService;

    @Override
    public String buildPrivilegeCode(String moduleCode,
                                     String submoduleCode,
                                     String featureTypeCode,
                                     String featureCode,
                                     String actionCode) {
        return privilegeService.buildPrivilegeCode(
                moduleCode,
                submoduleCode,
                featureTypeCode,
                featureCode,
                actionCode
        );
    }

    @Override
    public boolean hasPrivilege(String username, String privilegeCode) {
        if (username == null || username.isBlank() || privilegeCode == null || privilegeCode.isBlank()) {
            return false;
        }
        return privilegeService.getUserPrivilegeCodes(username).contains(privilegeCode);
    }

    @Override
    public boolean hasPrivilege(String username,
                                String moduleCode,
                                String submoduleCode,
                                String featureTypeCode,
                                String featureCode,
                                String actionCode) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return hasPrivilege(username, buildPrivilegeCode(
                moduleCode,
                submoduleCode,
                featureTypeCode,
                featureCode,
                actionCode
        ));
    }

    @Override
    public Set<String> getPrivilegeCodes(String username) {
        return privilegeService.getUserPrivilegeCodes(username);
    }
}
