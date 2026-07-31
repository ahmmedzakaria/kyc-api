package com.nexacore.systemmodule.layout.service.interfaces;

import com.nexacore.systemmodule.layout.dto.LayoutContextDto;

import java.util.Set;

public interface LayoutContextService {
    LayoutContextDto getEffectiveLayout(String clientCode, String username, Set<String> privilegeCodes);
    LayoutContextDto getPublicLayout(String clientCode, String origin);
}
