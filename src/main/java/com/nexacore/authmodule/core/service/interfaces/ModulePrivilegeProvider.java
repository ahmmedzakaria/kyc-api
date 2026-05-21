package com.nexacore.authmodule.core.service.interfaces;

import com.nexacore.authmodule.core.dto.PrivilegeFeatureDefinitionDto;

import java.util.List;

public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
