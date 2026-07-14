package com.nexacore.systemmodule.privilege.catalog.enums;

import lombok.Getter;

@Getter
public enum ApplicationModule {
    APP_CONFIG("05", "App Config"),
    COMMON("06", "Common"),
    ESB("07", "ESB"),
    GATEWAY("08", "Gateway"),
    KYC("01", "KYC"),
    AUTH("02", "Auth"),
    GIS("03", "GIS"),
    LOG("09", "Log"),
    POS("10", "POS"),
    SERVICES("04", "Services"),
    SYSTEM("11", "System");

    private final String code;
    private final String displayName;

    ApplicationModule(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
