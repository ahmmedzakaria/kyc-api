package com.nexacore.authmodule.service.interfaces;

import com.nexacore.authmodule.dto.PrivilegeFeatureDefinitionDto;

import java.util.List;

public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
