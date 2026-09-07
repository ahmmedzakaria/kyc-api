package com.nexacore.systemmodule.privilege.catalog.enums;

import lombok.Getter;

@Getter
public enum FeatureType {
    SETUP("01", "Setup", "fa fa-sliders"),
    OPERATIONS("02", "Operations", "fa fa-briefcase"),
    REPORT("03", "Report", "fa fa-chart-line");

    private final String code;
    private final String displayName;
    private final String defaultIcon;

    FeatureType(String code, String displayName, String defaultIcon) {
        this.code = code;
        this.displayName = displayName;
        this.defaultIcon = defaultIcon;
    }
}
