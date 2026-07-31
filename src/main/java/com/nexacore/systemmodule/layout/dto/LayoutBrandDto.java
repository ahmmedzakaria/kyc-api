package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LayoutBrandDto {
    private String displayName;
    private String shortName;
    private String logoUrl;
    private String logoDarkUrl;
    private String faviconUrl;
    private String supportUrl;
}
