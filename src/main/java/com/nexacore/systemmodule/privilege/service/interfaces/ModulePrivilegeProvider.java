package com.nexacore.systemmodule.privilege.service.interfaces;

import com.nexacore.systemmodule.privilege.dto.PrivilegeFeatureDefinitionDto;

import java.util.List;

public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
