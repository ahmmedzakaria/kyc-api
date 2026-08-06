package com.nexacore.systemmodule.accesscontrol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ApiRegistrySyncReportDto {
    private int added;
    private int changed;
    private int unchanged;
    private int deactivated;
    private int conflicted;
    @Builder.Default
    private List<String> conflicts = List.of();
    @Builder.Default
    private List<ApiRegistryDto> records = List.of();
}
