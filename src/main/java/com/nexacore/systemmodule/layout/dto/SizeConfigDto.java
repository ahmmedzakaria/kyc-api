package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeConfigDto {
    private Double spaceUnit;
    private Double radiusBase;
    private Double fontSizeBase;
    private Double headerHeight;
    private Double statusBarHeight;
    private Double railWidthCollapsed;
    private Double railWidthExpanded;
}
