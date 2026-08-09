package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import jakarta.servlet.http.HttpServletRequest;

public interface ClientRateLimiter {
    ClientRateLimitDecision check(SysAccClientApplication application, HttpServletRequest request);
}
