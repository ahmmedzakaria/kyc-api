package com.nexacore.kycmodule.person.privilege;

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
public class KycPrivilegeProvider implements ModulePrivilegeProvider {

    private static final String PERSON_FEATURE_CODE = "001";
    private static final String KYC_RECORD_FEATURE_CODE = "002";
    private static final String KYC_REPORT_FEATURE_CODE = "003";

    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        return List.of(
                personFeature(),
                kycRecordFeature(),
                kycReportFeature()
        );
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

    private PrivilegeFeatureDefinitionDto kycRecordFeature() {
        return feature(
                FeatureType.OPERATIONS,
                KYC_RECORD_FEATURE_CODE,
                "KYC Record",
                "KYC",
                "fa fa-id-card",
                List.of(
                        action(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.CREATE),
                        action(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.UPDATE),
                        action(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.DELETE),
                        action(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.VIEW),
                        action(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.SEARCH)
                ),
                List.of(
                        menuItem("All Records", "/kyc", "fa fa-list", List.of(
                                privilegeCode(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.VIEW),
                                privilegeCode(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.SEARCH)
                        )),
                        menuItem("Create Record", "/kyc/create", "fa fa-plus", List.of(
                                privilegeCode(FeatureType.OPERATIONS, KYC_RECORD_FEATURE_CODE, PrivilegeAction.CREATE)
                        ))
                )
        );
    }

    private PrivilegeFeatureDefinitionDto kycReportFeature() {
        return feature(
                FeatureType.REPORT,
                KYC_REPORT_FEATURE_CODE,
                "KYC Report",
                "KYC Reports",
                "fa fa-chart-line",
                List.of(
                        action(FeatureType.REPORT, KYC_REPORT_FEATURE_CODE, PrivilegeAction.VIEW),
                        action(FeatureType.REPORT, KYC_REPORT_FEATURE_CODE, PrivilegeAction.SEARCH)
                ),
                List.of(
                        menuItem("KYC Reports", "/kyc", "fa fa-chart-simple", List.of(
                                privilegeCode(FeatureType.REPORT, KYC_REPORT_FEATURE_CODE, PrivilegeAction.VIEW),
                                privilegeCode(FeatureType.REPORT, KYC_REPORT_FEATURE_CODE, PrivilegeAction.SEARCH)
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
        return ApplicationModule.KYC.getCode() + featureType.getCode() + featureCode + action.getCode();
    }
}
