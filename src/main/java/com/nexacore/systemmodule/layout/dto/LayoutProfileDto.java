package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.LayoutDensity;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.enums.NavigationMode;
import com.nexacore.systemmodule.layout.enums.ThemeMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutProfileDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private LayoutType layoutType;
    private NavigationMode navigationMode;
    private ThemeMode themeMode;
    private LayoutDensity density;
    private Boolean topbarEnabled;
    private Boolean sidebarEnabled;
    private Boolean sidebarCollapsed;
    private Boolean footerEnabled;
    private Boolean breadcrumbEnabled;
    private Boolean commandBarEnabled;
    private Boolean rtlEnabled;
    private Boolean active;
    private LayoutBrandDto brand;
    private List<ThemeConfigEntryDto> themes;
    private SizeConfigDto sizes;
    private FontConfigDto fonts;
}
