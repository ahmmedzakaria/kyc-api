package com.nexacore.systemmodule.layout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NavNodeDto {
    private String code;
    private String tCode;
    private String label;
    private String type;
    private String icon;
    private String route;

    @Builder.Default
    private List<String> privilegeCodes = new ArrayList<>();

    @Builder.Default
    private List<NavNodeDto> children = new ArrayList<>();
}
