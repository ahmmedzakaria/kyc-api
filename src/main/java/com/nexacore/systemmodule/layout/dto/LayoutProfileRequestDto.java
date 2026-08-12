package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.LayoutDensity;
import com.nexacore.systemmodule.layout.enums.LayoutType;
import com.nexacore.systemmodule.layout.enums.NavigationMode;
import com.nexacore.systemmodule.layout.enums.ThemeMode;
import lombok.Data;

import java.util.List;

@Data
public class LayoutProfileRequestDto {
    private Long id;
    private String profileCode;
    private String profileName;
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
    /** Null = leave existing themes untouched; non-null (incl. empty) = full-replace. */
    private List<ThemeConfigEntryDto> themes;
    private SizeConfigDto sizes;
    private FontConfigDto fonts;
}
