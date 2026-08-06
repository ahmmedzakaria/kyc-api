package com.nexacore.commonmodule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutePrivilegePolicyDto {
    private String routeUrl;
    private String matchMode;

    @Builder.Default
    private Set<String> privilegeCodes = new LinkedHashSet<>();
}
