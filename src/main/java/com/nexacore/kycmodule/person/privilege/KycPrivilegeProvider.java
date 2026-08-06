package com.nexacore.kycmodule.person.privilege;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeActionDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;
import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeMenuItemDto;
import com.nexacore.systemmodule.privilege.catalog.service.interfaces.ModulePrivilegeProvider;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationModule;
import com.nexacore.systemmodule.privilege.catalog.enums.ApplicationSubmodule;
import com.nexacore.systemmodule.privilege.catalog.enums.FeatureType;
import com.nexacore.systemmodule.privilege.catalog.enums.PrivilegeAction;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KycPrivilegeProvider implements ModulePrivilegeProvider {

    private static final String PERSON_FEATURE_CODE = "001";
    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        return List.of(personFeature());
    }

    private PrivilegeFeatureDefinitionDto personFeature() {
        return feature(
                FeatureType.OPERATIONS,
                PERSON_FEATURE_CODE,
                "Person",
                "Person",
                "fa fa-users",
                List.of(
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.CREATE),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.UPDATE),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.DELETE),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.APPROVE),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.REJECT),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.SEND_BACK),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.VIEW),
                        action(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.SEARCH)
                ),
                List.of(
                        menuItem("Person List", "/person", "fa fa-list", List.of(
                                privilegeCode(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.VIEW),
                                privilegeCode(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.SEARCH)
                        )),
                        menuItem("Add Person", "/person/create", "fa fa-user-plus", List.of(
                                privilegeCode(FeatureType.OPERATIONS, PERSON_FEATURE_CODE, PrivilegeAction.CREATE)
                        ))
                )
        );
    }

    private PrivilegeFeatureDefinitionDto feature(FeatureType featureType,
                                                  String featureCode,
                                                  String featureName,
                                                  String menuLabel,
                                                  String icon,
                                                  List<PrivilegeActionDefinitionDto> actions,
                                                  List<PrivilegeMenuItemDto> menuItems) {
        return PrivilegeFeatureDefinitionDto.builder()
                .moduleCode(ApplicationModule.KYC.getCode())
                .moduleName(ApplicationModule.KYC.getDisplayName())
                .submoduleCode(ApplicationSubmodule.KYC_PERSON.getCode())
                .submoduleName(ApplicationSubmodule.KYC_PERSON.getDisplayName())
                .featureTypeCode(featureType.getCode())
                .featureTypeName(featureType.getDisplayName())
                .featureCode(featureCode)
                .featureName(featureName)
                .menuLabel(menuLabel)
                .icon(icon)
                .actions(actions)
                .menuItems(menuItems)
                .build();
    }

    private PrivilegeActionDefinitionDto action(FeatureType featureType, String featureCode, PrivilegeAction action) {
        return PrivilegeActionDefinitionDto.builder()
                .actionCode(action.getCode())
                .actionName(action.getDisplayName())
                .privilegeCode(privilegeCode(featureType, featureCode, action))
                .build();
    }

    private PrivilegeMenuItemDto menuItem(String label, String path, String icon, List<String> privilegeCodes) {
        return PrivilegeMenuItemDto.builder()
                .label(label)
                .path(path)
                .icon(icon)
                .privilegeCodes(privilegeCodes)
                .build();
    }

    private String privilegeCode(FeatureType featureType, String featureCode, PrivilegeAction action) {
        return ApplicationModule.KYC.getCode()
                + ApplicationSubmodule.KYC_PERSON.getCode()
                + featureType.getCode()
                + featureCode
                + action.getCode();
    }
}
