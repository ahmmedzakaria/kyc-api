package com.nexacore.systemmodule.privilege.bootstrap;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeActionDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.enums.PrivilegeAction;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.ModulePrivilegeProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BootstrapAdministrationPrivilegeProvider implements ModulePrivilegeProvider {

    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        return List.of(
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "001", "Client Application",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "002", "Client Credential",
                        PrivilegeAction.ROTATE),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "003", "Client API Permission",
                        PrivilegeAction.ASSIGN),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "004", "Client Feature Permission",
                        PrivilegeAction.ASSIGN),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "005", "Client Tenant Assignment",
                        PrivilegeAction.ASSIGN),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "006", "API Registry",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE, PrivilegeAction.SYNCHRONIZE),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "007", "User Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE, PrivilegeAction.ASSIGN),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "008", "Role Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "009", "Tenant Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.CREATE, PrivilegeAction.SYNCHRONIZE, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_ACCESS_CONTROL, "010", "System Dashboard",
                        PrivilegeAction.VIEW),
                feature(ApplicationSubmodule.SYS_PRIVILEGE, "001", "Privilege Catalog",
                        PrivilegeAction.VIEW, PrivilegeAction.SYNCHRONIZE, PrivilegeAction.ASSIGN),
                feature(ApplicationSubmodule.SYSTEM_LAYOUT, "001", "Layout Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_WORKFLOW, "001", "Workflow Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_BACKUP, "001", "Database Backup Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.EXECUTE, PrivilegeAction.EXPORT,
                        PrivilegeAction.SYNCHRONIZE, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.SYSTEM_LICENSE, "999", "License Administration",
                        PrivilegeAction.VIEW, PrivilegeAction.MANAGE),
                feature(ApplicationSubmodule.LOG_ADMINISTRATION, "001", "Error Log",
                        PrivilegeAction.VIEW),
                feature(ApplicationSubmodule.LOG_ADMINISTRATION, "002", "Access Log",
                        PrivilegeAction.VIEW),
                feature(ApplicationSubmodule.LOG_ADMINISTRATION, "003", "Audit Log",
                        PrivilegeAction.VIEW),
                feature(ApplicationSubmodule.LOG_ADMINISTRATION, "004", "Log Dashboard",
                        PrivilegeAction.VIEW)
        );
    }

    private PrivilegeFeatureDefinitionDto feature(ApplicationSubmodule submodule,
                                                  String featureCode,
                                                  String featureName,
                                                  PrivilegeAction... actions) {
        return PrivilegeFeatureDefinitionDto.builder()
                .moduleCode(submodule.getModule().getCode())
                .moduleName(submodule.getModule().getDisplayName())
                .submoduleCode(submodule.getCode())
                .submoduleName(submodule.getDisplayName())
                .featureTypeCode(FeatureType.SETUP.getCode())
                .featureTypeName(FeatureType.SETUP.getDisplayName())
                .featureCode(featureCode)
                .featureName(featureName)
                .actions(List.of(actions).stream()
                        .map(action -> PrivilegeActionDefinitionDto.builder()
                                .actionCode(action.getCode())
                                .actionName(action.getDisplayName())
                                .privilegeCode(privilegeCode(submodule, featureCode, action))
                                .build())
                        .toList())
                .build();
    }

    private String privilegeCode(ApplicationSubmodule submodule,
                                 String featureCode,
                                 PrivilegeAction action) {
        return submodule.getModule().getCode()
                + submodule.getCode()
                + FeatureType.SETUP.getCode()
                + featureCode
                + action.getCode();
    }
}
