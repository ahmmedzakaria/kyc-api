package com.nexacore.authmodule.core.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AdministrativePrivilegeCharacterizationTest {

    @Test
    void currentGlobalRoleAuthorizationCouplesAdministrationToTenantView() throws Exception {
        Method method = RoleController.class.getMethod("saveGlobalRole",
                com.nexacore.authmodule.core.dto.RoleRequestDto.class);

        assertThat(method.getAnnotation(PreAuthorize.class).value())
                .contains("TENANT_VIEW")
                .contains("ROLE_ADMINISTRATION_MANAGE");
    }
}
