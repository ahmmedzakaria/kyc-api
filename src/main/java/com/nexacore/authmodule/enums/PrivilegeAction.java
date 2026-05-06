package com.nexacore.authmodule.enums;

import lombok.Getter;

@Getter
public enum PrivilegeAction {
    CREATE("01", "Create"),
    UPDATE("02", "Update"),
    DELETE("03", "Delete"),
    REJECT("04", "Reject"),
    SEND_BACK("05", "Send Back"),
    VIEW("06", "View"),
    APPROVE("07", "Approve"),
    SEARCH("08", "Search");

    private final String code;
    private final String displayName;

    PrivilegeAction(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
