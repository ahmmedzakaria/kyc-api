package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.systemmodule.privilege.dto.PrivilegeActionDefinitionDto;
import com.nexacore.systemmodule.privilege.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.dto.PrivilegeMenuItemDto;
import com.nexacore.systemmodule.privilege.service.interfaces.ModulePrivilegeProvider;
import com.nexacore.systemmodule.privilege.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.enums.FeatureType;
import com.nexacore.systemmodule.privilege.enums.PrivilegeAction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthPrivilegeProvider implements ModulePrivilegeProvider {

    private static final String PRIVILEGE_FEATURE_CODE = "001";

    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        return List.of(privilegeManagementFeature());
    }

    private PrivilegeFeatureDefinitionDto privilegeManagementFeature() {
        return PrivilegeFeatureDefinitionDto.builder()
                .moduleCode(ApplicationModule.AUTH.getCode())
                .moduleName(ApplicationModule.AUTH.getDisplayName())
                .featureTypeCode(FeatureType.SETUP.getCode())
                .featureTypeName(FeatureType.SETUP.getDisplayName())
                .featureCode(PRIVILEGE_FEATURE_CODE)
                .featureName("Privilege Management")
                .menuLabel("Privilege Management")
                .icon("fa fa-shield-halved")
                .actions(List.of(
                        action(PrivilegeAction.CREATE),
                        action(PrivilegeAction.UPDATE),
                        action(PrivilegeAction.VIEW),
                        action(PrivilegeAction.SEARCH)
                ))
                .menuItems(List.of(
                        PrivilegeMenuItemDto.builder()
                                .label("Manage Privileges")
                                .path("/privileges")
                                .icon("fa fa-key")
                                .privilegeCodes(List.of(
                                        privilegeCode(PrivilegeAction.VIEW),
                                        privilegeCode(PrivilegeAction.SEARCH)
                                ))
                                .build()
                ))
                .build();
    }

    private PrivilegeActionDefinitionDto action(PrivilegeAction action) {
        return PrivilegeActionDefinitionDto.builder()
                .actionCode(action.getCode())
                .actionName(action.getDisplayName())
                .privilegeCode(privilegeCode(action))
                .build();
    }

    private String privilegeCode(PrivilegeAction action) {
        return ApplicationModule.AUTH.getCode()
                + FeatureType.SETUP.getCode()
                + PRIVILEGE_FEATURE_CODE
                + action.getCode();
    }
}
