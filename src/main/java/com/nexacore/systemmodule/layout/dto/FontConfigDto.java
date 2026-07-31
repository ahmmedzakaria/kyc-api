package com.nexacore.systemmodule.layout.dto;

import com.nexacore.systemmodule.layout.enums.FontSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FontConfigDto {
    private String bodyFamily;
    private String headingFamily;
    private String monoFamily;
    private FontSource fontSource;
    private String fontUrl;
    private String fallbackStack;
}
