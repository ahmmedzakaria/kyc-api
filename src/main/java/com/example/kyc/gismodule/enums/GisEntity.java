package com.example.kyc.gismodule.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum GisEntity {
    ADMINISTRATIVE_BOUNDARY_LEVEL_0("AdministrativeBoundariesLevel0", "ABL_0"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_1("AdministrativeBoundariesLevel1", "ABL_1"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_2("AdministrativeBoundariesLevel2", "ABL_2"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_3("AdministrativeBoundariesLevel3", "ABL_3"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_4("AdministrativeBoundariesLevel4", "ABL_4"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_5("AdministrativeBoundariesLevel5", "ABL_5"),
    ADMINISTRATIVE_BOUNDARY_LEVEL_6("AdministrativeBoundariesLevel6", "ABL_6");

    private final String domainName;
    private final String gisCode;

    public static GisEntity fromCode(String gisCode) {
        return Arrays.stream(values())
                .filter(entity -> entity.getGisCode().equalsIgnoreCase(gisCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported GIS code: " + gisCode));
    }
}
