package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutContextDto {
    private String activeProfileCode;

    @Builder.Default
    private List<LayoutProfileDto> availableProfiles = new ArrayList<>();

    @Builder.Default
    private List<NavNodeDto> navTree = new ArrayList<>();

    @Builder.Default
    private List<ThemeConfigEntryDto> themes = new ArrayList<>();

    private SizeConfigDto sizes;
    private FontConfigDto fonts;
}
