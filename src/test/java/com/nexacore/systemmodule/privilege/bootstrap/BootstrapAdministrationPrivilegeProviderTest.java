package com.nexacore.systemmodule.privilege.bootstrap;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapAdministrationPrivilegeProviderTest {

    private final BootstrapAdministrationPrivilegeProvider provider =
            new BootstrapAdministrationPrivilegeProvider();

    @Test
    void definesEveryBootstrapAdministrationPrivilegeWithUniqueElevenCharacterCodes() {
        Set<String> codes = provider.getPrivilegeFeatures().stream()
                .map(PrivilegeFeatureDefinitionDto::getActions)
                .flatMap(java.util.Collection::stream)
                .map(action -> action.getPrivilegeCode())
                .collect(Collectors.toSet());
        long declaredCodeCount = provider.getPrivilegeFeatures().stream()
                .map(PrivilegeFeatureDefinitionDto::getActions)
                .mapToLong(java.util.Collection::size)
                .sum();

        assertThat(codes)
                .hasSize((int) declaredCodeCount)
                .allMatch(code -> code.matches("\\d{11}"))
                .contains(
                        BootstrapAdministrationPrivileges.CLIENT_APPLICATION_VIEW,
                        BootstrapAdministrationPrivileges.CLIENT_APPLICATION_MANAGE,
                        BootstrapAdministrationPrivileges.CLIENT_CREDENTIAL_ROTATE,
                        BootstrapAdministrationPrivileges.CLIENT_API_PERMISSION_ASSIGN,
                        BootstrapAdministrationPrivileges.CLIENT_FEATURE_PERMISSION_ASSIGN,
                        BootstrapAdministrationPrivileges.CLIENT_TENANT_ASSIGN,
                        BootstrapAdministrationPrivileges.API_REGISTRY_VIEW,
                        BootstrapAdministrationPrivileges.API_REGISTRY_MANAGE,
                        BootstrapAdministrationPrivileges.API_REGISTRY_SYNCHRONIZE,
                        BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_VIEW,
                        BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_SYNCHRONIZE,
                        BootstrapAdministrationPrivileges.PRIVILEGE_CATALOG_ASSIGN,
                        BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_VIEW,
                        BootstrapAdministrationPrivileges.LAYOUT_ADMINISTRATION_MANAGE,
                        BootstrapAdministrationPrivileges.WORKFLOW_ADMINISTRATION_VIEW,
                        BootstrapAdministrationPrivileges.WORKFLOW_ADMINISTRATION_MANAGE,
                        BootstrapAdministrationPrivileges.LICENSE_ADMINISTRATION_VIEW,
                        BootstrapAdministrationPrivileges.LICENSE_ADMINISTRATION_MANAGE,
                        BootstrapAdministrationPrivileges.DATABASE_BACKUP_VIEW,
                        BootstrapAdministrationPrivileges.DATABASE_BACKUP_EXECUTE,
                        BootstrapAdministrationPrivileges.DATABASE_BACKUP_DOWNLOAD,
                        BootstrapAdministrationPrivileges.DATABASE_BACKUP_DELIVER,
                        BootstrapAdministrationPrivileges.DATABASE_BACKUP_MANAGE
                );
    }
}
