package com.nexacore.systemmodule.privilege.enums;

import lombok.Getter;

@Getter
public enum ApplicationSubmodule {
    KYC_PERSON("01", "Person"),
    SYS_PRIVILEGE("01", "Privilege");

    private final String code;
    private final String displayName;

    ApplicationSubmodule(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
