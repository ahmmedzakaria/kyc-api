package com.nexacore.gatewaymodule.layout.dto;

import java.util.List;

public record PublicLayoutContextDto(
        String activeProfileCode,
        List<PublicLayoutProfileDto> availableProfiles,
        List<PublicThemeDto> themes,
        PublicSizeConfigDto sizes,
        PublicFontConfigDto fonts
) {
    public PublicLayoutContextDto {
        availableProfiles = availableProfiles == null ? List.of() : List.copyOf(availableProfiles);
        themes = themes == null ? List.of() : List.copyOf(themes);
    }
}
