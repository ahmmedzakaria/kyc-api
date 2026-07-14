package com.nexacore.systemmodule.privilege.catalog.enums;

import lombok.Getter;

@Getter
public enum FeatureType {
    SETUP("01", "Setup"),
    OPERATIONS("02", "Operations"),
    REPORT("03", "Report");

    private final String code;
    private final String displayName;

    FeatureType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
