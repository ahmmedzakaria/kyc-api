package com.nexacore.authmodule.dto;

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
public class SidebarMenuDto {
    private String label;
    private String icon;
    private String path;

    @Builder.Default
    private List<String> privilegeCodes = new ArrayList<>();

    @Builder.Default
    private List<SidebarMenuDto> children = new ArrayList<>();
}
