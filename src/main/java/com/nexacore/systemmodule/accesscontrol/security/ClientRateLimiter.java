package com.nexacore.systemmodule.accesscontrol.security;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import jakarta.servlet.http.HttpServletRequest;

public interface ClientRateLimiter {
    ClientRateLimitDecision check(SysPrivClientApplication application, HttpServletRequest request);
}
