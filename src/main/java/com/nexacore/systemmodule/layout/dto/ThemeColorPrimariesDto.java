package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeColorPrimariesDto {
    private String text;
    private String paper;
    private String card;
    private String accent;
    private String amber;
    private String red;
    private String success;
    private String info;
}
