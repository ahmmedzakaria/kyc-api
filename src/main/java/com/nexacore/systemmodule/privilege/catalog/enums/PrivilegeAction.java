package com.nexacore.systemmodule.privilege.catalog.enums;

import lombok.Getter;

@Getter
public enum PrivilegeAction {
    // Read-only and low-impact actions: 01-09
    VIEW("01", "View"),             // Read a specific record or detailed resource.
    SEARCH("02", "Search"),         // List, filter, or query multiple resources.
    VALIDATE("03", "Validate"),     // Check validity or eligibility without changing state.
    EXPORT("04", "Export"),         // Download or extract data from the system.

    // Data creation and modification actions: 10-19
    CREATE("10", "Create"),         // Add a new business or configuration record.
    IMPORT("11", "Import"),         // Create or update records from an external data source.
    UPDATE("12", "Update"),         // Modify an existing record without changing its lifecycle state.
    EXECUTE("13", "Execute"),       // Run a business operation, command, job, or process.

    // Workflow decision actions: 20-29
    SEND_BACK("20", "Send Back"),   // Return workflow work to an earlier step for correction.
    APPROVE("21", "Approve"),       // Accept a pending workflow decision or request.
    REJECT("22", "Reject"),         // Decline a pending workflow decision or request.
    PUBLISH("23", "Publish"),       // Make an approved draft or version available for use.

    // Lifecycle actions: 30-39
    ACTIVATE("30", "Activate"),     // Enable an inactive resource for operational use.
    SUSPEND("31", "Suspend"),       // Temporarily prevent use while preserving the resource.
    CANCEL("32", "Cancel"),         // Stop an in-progress operation or agreement.
    RETIRE("33", "Retire"),         // Permanently end future operational use while retaining history.
    ARCHIVE("34", "Archive"),       // Move an inactive resource out of normal views for retention.

    // Destructive actions: 40-49
    DELETE("40", "Delete"),         // Soft-delete or remove a resource with recovery still possible.
    PURGE("41", "Purge"),           // Irreversibly erase or anonymize a resource and related data.

    // System-administration actions: 80-89
    SYNCHRONIZE("80", "Synchronize"), // Reconcile generated or external metadata with stored state.
    ASSIGN("81", "Assign"),           // Grant or replace a role, privilege, scope, or ownership relation.
    GENERATE("82", "Generate"),       // Create a system-issued key, secret, token, or artifact.
    ROTATE("83", "Rotate"),           // Replace a credential and retire its previous value safely.
    REVOKE("84", "Revoke"),           // Invalidate an active credential, token, grant, or permission.
    OVERRIDE("85", "Override"),       // Bypass or replace a standard policy decision with authority.
    IMPERSONATE("86", "Impersonate"), // Temporarily act as another user for controlled support purposes.
    MANAGE("87", "Manage");           // Perform broad administration when narrower actions do not apply.

    private final String code;
    private final String displayName;

    PrivilegeAction(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
}
