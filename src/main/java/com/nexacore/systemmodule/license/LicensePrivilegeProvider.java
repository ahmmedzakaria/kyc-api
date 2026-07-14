package com.nexacore.systemmodule.license;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeActionDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeMenuItemDto;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.enums.PrivilegeAction;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.ModulePrivilegeProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LicensePrivilegeProvider implements ModulePrivilegeProvider {

    private static final String PLAN_FEATURE_CODE = "001";
    private static final String SUBSCRIPTION_FEATURE_CODE = "001";
    private static final String KEY_FEATURE_CODE = "002";
    private static final String ACTIVATION_FEATURE_CODE = "003";
    private static final String USAGE_FEATURE_CODE = "001";
    private static final String AUDIT_FEATURE_CODE = "002";

    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        return List.of(
                feature(FeatureType.SETUP, PLAN_FEATURE_CODE, "License Plan", "License Plans", "/system/license/plans",
                        List.of(PrivilegeAction.CREATE, PrivilegeAction.UPDATE, PrivilegeAction.VIEW, PrivilegeAction.SEARCH)),
                feature(FeatureType.OPERATIONS, SUBSCRIPTION_FEATURE_CODE, "License Subscription", "Subscriptions", "/system/license/subscriptions",
                        List.of(PrivilegeAction.CREATE, PrivilegeAction.UPDATE, PrivilegeAction.VIEW, PrivilegeAction.SEARCH)),
                feature(FeatureType.OPERATIONS, KEY_FEATURE_CODE, "License Key", "License Keys", "/system/license/keys",
                        List.of(PrivilegeAction.CREATE, PrivilegeAction.VIEW, PrivilegeAction.SEARCH)),
                feature(FeatureType.OPERATIONS, ACTIVATION_FEATURE_CODE, "License Activation", "Activations", "/system/license/activations",
                        List.of(PrivilegeAction.CREATE, PrivilegeAction.VIEW, PrivilegeAction.SEARCH)),
                feature(FeatureType.REPORT, USAGE_FEATURE_CODE, "License Usage", "License Usage", "/system/license/usage",
                        List.of(PrivilegeAction.VIEW, PrivilegeAction.SEARCH)),
                feature(FeatureType.REPORT, AUDIT_FEATURE_CODE, "License Audit", "License Audit", "/system/license/audit",
                        List.of(PrivilegeAction.VIEW, PrivilegeAction.SEARCH))
        );
    }

    private PrivilegeFeatureDefinitionDto feature(FeatureType featureType,
                                                  String featureCode,
                                                  String featureName,
                                                  String menuLabel,
                                                  String path,
                                                  List<PrivilegeAction> actions) {
        return PrivilegeFeatureDefinitionDto.builder()
                .moduleCode(ApplicationModule.SYSTEM.getCode())
                .moduleName(ApplicationModule.SYSTEM.getDisplayName())
                .submoduleCode(ApplicationSubmodule.SYSTEM_LICENSE.getCode())
                .submoduleName(ApplicationSubmodule.SYSTEM_LICENSE.getDisplayName())
                .featureTypeCode(featureType.getCode())
                .featureTypeName(featureType.getDisplayName())
                .featureCode(featureCode)
                .featureName(featureName)
                .menuLabel(menuLabel)
                .icon("fa fa-certificate")
                .actions(actions.stream()
                        .map(action -> action(featureType, featureCode, action))
                        .toList())
                .menuItems(List.of(PrivilegeMenuItemDto.builder()
                        .label(menuLabel)
                        .path(path)
                        .icon("fa fa-certificate")
                        .privilegeCodes(List.of(
                                privilegeCode(featureType, featureCode, PrivilegeAction.VIEW),
                                privilegeCode(featureType, featureCode, PrivilegeAction.SEARCH)
                        ))
                        .build()))
                .build();
    }

    private PrivilegeActionDefinitionDto action(FeatureType featureType, String featureCode, PrivilegeAction action) {
        return PrivilegeActionDefinitionDto.builder()
                .actionCode(action.getCode())
                .actionName(action.getDisplayName())
                .privilegeCode(privilegeCode(featureType, featureCode, action))
                .build();
    }

    private String privilegeCode(FeatureType featureType, String featureCode, PrivilegeAction action) {
        return ApplicationModule.SYSTEM.getCode()
                + ApplicationSubmodule.SYSTEM_LICENSE.getCode()
                + featureType.getCode()
                + featureCode
                + action.getCode();
    }
}
