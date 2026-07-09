package com.nexacore.authmodule.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationContextDto {
    private String clientCode;
    private String clientType;

    @Builder.Default
    private List<SidebarMenuDto> menus = new ArrayList<>();

    @Builder.Default
    private Set<String> privilegeCodes = new HashSet<>();

    @Builder.Default
    private Set<String> enabledModules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledSubmodules = new HashSet<>();

    @Builder.Default
    private Set<String> enabledFeatures = new HashSet<>();
}
