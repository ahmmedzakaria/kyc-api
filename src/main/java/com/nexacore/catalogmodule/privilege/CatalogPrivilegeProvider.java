package com.nexacore.catalogmodule.privilege;

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
public class CatalogPrivilegeProvider implements ModulePrivilegeProvider {
    private static final String FEATURE_CODE = "001";

    @Override
    public List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures() {
        List<PrivilegeActionDefinitionDto> actions = List.of(
                action(PrivilegeAction.VIEW), action(PrivilegeAction.SEARCH),
                action(PrivilegeAction.CREATE), action(PrivilegeAction.UPDATE),
                action(PrivilegeAction.PUBLISH), action(PrivilegeAction.DELETE));
        return List.of(PrivilegeFeatureDefinitionDto.builder()
                .moduleCode(ApplicationModule.CATALOG.getCode()).moduleName(ApplicationModule.CATALOG.getDisplayName())
                .submoduleCode(ApplicationSubmodule.CATALOG_CONTENT.getCode())
                .submoduleName(ApplicationSubmodule.CATALOG_CONTENT.getDisplayName())
                .featureTypeCode(FeatureType.SETUP.getCode()).featureTypeName(FeatureType.SETUP.getDisplayName())
                .featureCode(FEATURE_CODE).featureName("Catalog Content")
                .menuLabel("Catalog").icon("fa fa-store").actions(actions)
                .menuItems(List.of(PrivilegeMenuItemDto.builder().label("Catalog Content")
                        .path("/catalog").icon("fa fa-store")
                        .privilegeCodes(List.of(code(PrivilegeAction.VIEW), code(PrivilegeAction.SEARCH))).build()))
                .build());
    }

    private PrivilegeActionDefinitionDto action(PrivilegeAction action) {
        return PrivilegeActionDefinitionDto.builder().actionCode(action.getCode())
                .actionName(action.getDisplayName()).privilegeCode(code(action)).build();
    }

    private String code(PrivilegeAction action) {
        return ApplicationModule.CATALOG.getCode() + ApplicationSubmodule.CATALOG_CONTENT.getCode()
                + FeatureType.SETUP.getCode() + FEATURE_CODE + action.getCode();
    }
}
