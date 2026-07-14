package com.nexacore.systemmodule.privilege.catalog.service.interfaces;

import com.nexacore.systemmodule.privilege.catalog.dto.PrivilegeFeatureDefinitionDto;

import java.util.List;

public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
