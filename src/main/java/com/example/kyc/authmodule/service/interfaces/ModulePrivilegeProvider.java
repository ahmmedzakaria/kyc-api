package com.example.kyc.authmodule.service.interfaces;

import com.example.kyc.authmodule.dto.PrivilegeFeatureDefinitionDto;

import java.util.List;

public interface ModulePrivilegeProvider {
    List<PrivilegeFeatureDefinitionDto> getPrivilegeFeatures();
}
