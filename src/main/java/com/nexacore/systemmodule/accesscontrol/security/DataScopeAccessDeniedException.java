package com.nexacore.systemmodule.accesscontrol.security;

import org.springframework.security.access.AccessDeniedException;

public class DataScopeAccessDeniedException extends AccessDeniedException {
    public DataScopeAccessDeniedException(String message) {
        super(message);
    }
}
