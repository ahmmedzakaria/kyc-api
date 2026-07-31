package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeChromeOverridesDto {
    private String accentSoft;
    private String bg;
    private String border;
    private String borderStrong;
    private String hoverBg;
    private String activeBg;
    private String searchBg;
    private String searchBorder;
    private String searchText;
    private String searchPlaceholder;
}
