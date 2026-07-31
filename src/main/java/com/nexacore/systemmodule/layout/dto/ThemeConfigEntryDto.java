package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeConfigEntryDto {
    private String id;
    private String label;
    private String base;
    private String swatch;
    private ThemeColorPrimariesDto primaries;
    private ThemeChromeOverridesDto chromeOverrides;
}
