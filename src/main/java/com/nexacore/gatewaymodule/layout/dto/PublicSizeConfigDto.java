package com.nexacore.gatewaymodule.layout.dto;

public record PublicSizeConfigDto(
        Double spaceUnit,
        Double radiusBase,
        Double fontSizeBase,
        Double headerHeight,
        Double statusBarHeight,
        Double railWidthCollapsed,
        Double railWidthExpanded
) {}
