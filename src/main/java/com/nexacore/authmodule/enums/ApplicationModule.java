package com.nexacore.authmodule.enums;

import lombok.Getter;

@Getter
public enum ApplicationModule {
    KYC("01", "KYC"),
    AUTH("02", "Auth"),
    GIS("03", "GIS"),
    SERVICES("04", "Services");

    private final String code;
    private final String displayName;

    ApplicationModule(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
