package com.nexacore.gatewaymodule.privilege.service.interfaces;

import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

import java.util.Set;

public interface PrivilegeModuleGateway extends ModuleGateway {
    String buildPrivilegeCode(String moduleCode,
                              String submoduleCode,
                              String featureTypeCode,
                              String featureCode,
                              String actionCode);

    boolean hasPrivilege(String username, String privilegeCode);

    boolean hasPrivilege(String username,
                         String moduleCode,
                         String submoduleCode,
                         String featureTypeCode,
                         String featureCode,
                         String actionCode);

    Set<String> getPrivilegeCodes(String username);
}

