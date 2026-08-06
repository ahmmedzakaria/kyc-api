package com.nexacore.systemmodule.accesscontrol.enums;

public enum ClientApplicationType {
    WEB,
    MOBILE,
    POS,
    ERP,
    PARTNER_PORTAL,
    INTERNAL_SERVICE;

    public boolean isConfidential() {
        return switch (this) {
            case WEB, MOBILE -> false;
            case POS, ERP, PARTNER_PORTAL, INTERNAL_SERVICE -> true;
        };
    }
}
