package com.nexacore.systemmodule.privilege.catalog.enums;

import lombok.Getter;

@Getter
public enum ApplicationSubmodule {
    KYC_PERSON(ApplicationModule.KYC, "01", "Person"),
    SYS_PRIVILEGE(ApplicationModule.AUTH, "01", "Privilege"),
    SYSTEM_LICENSE(ApplicationModule.SYSTEM, "03", "License"),
    SERVICE_CACHE(ApplicationModule.SERVICES, "01", "Cache Service"),
    SERVICE_EMAIL(ApplicationModule.SERVICES, "02", "Email Service"),
    SERVICE_FILE(ApplicationModule.SERVICES, "03", "File Service"),
    SERVICE_FIREBASE_AUTH(ApplicationModule.SERVICES, "04", "Firebase Auth Service"),
    SERVICE_MESSAGING(ApplicationModule.SERVICES, "05", "Messaging Service"),
    SERVICE_QUEUEING(ApplicationModule.SERVICES, "06", "Queueing Service"),
    SERVICE_REPORT(ApplicationModule.SERVICES, "07", "Report Service");

    private final ApplicationModule module;
    private final String code;
    private final String displayName;

    ApplicationSubmodule(ApplicationModule module, String code, String displayName) {
        this.module = module;
        this.code = code;
        this.displayName = displayName;
    }
}
