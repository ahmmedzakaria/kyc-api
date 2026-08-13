package com.nexacore.gatewaymodule.auth.service.interfaces;

import com.nexacore.gatewaymodule.auth.dto.AuthUserAccessDto;
import com.nexacore.gatewaymodule.service.interfaces.ModuleGateway;

public interface AuthModuleGateway extends ModuleGateway {
    AuthUserAccessDto getUserAccess(String username);

    AuthUserAccessDto getUserAccess(Long accountId, Long tenantId);

    Long getUserId(String username);

    void requireUserExists(Long userId);

    void requireRoleExists(Long roleId);

    long countUsersByTenantId(Long tenantId);
}
