package com.nexacore.systemmodule.privilege.catalog.dto;

public record PrivilegeDeactivateRequest(String privilegeCode, String version, boolean dependenciesPresented) {}
