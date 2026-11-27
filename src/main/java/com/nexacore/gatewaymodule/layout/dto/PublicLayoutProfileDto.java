package com.nexacore.gatewaymodule.layout.dto;

public record PublicLayoutProfileDto(
        String code,
        String name,
        String layoutType,
        String navigationMode,
        String themeMode,
        String density,
        boolean topbarEnabled,
        boolean sidebarEnabled,
        boolean sidebarCollapsed,
        boolean footerEnabled,
        boolean breadcrumbEnabled,
        boolean commandBarEnabled,
        boolean rtlEnabled,
        PublicLayoutBrandDto brand
) {}
