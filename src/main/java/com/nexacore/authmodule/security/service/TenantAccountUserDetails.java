package com.nexacore.authmodule.security.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public final class TenantAccountUserDetails extends User {
    private final Long accountId;
    private final Long tenantId;

    public TenantAccountUserDetails(Long accountId, Long tenantId, String username, String password,
                                    boolean enabled, boolean accountNonLocked,
                                    Collection<? extends GrantedAuthority> authorities) {
        super(username, password == null ? "" : password, enabled, true, true, accountNonLocked, authorities);
        this.accountId = accountId;
        this.tenantId = tenantId;
    }

    public Long accountId() { return accountId; }
    public Long tenantId() { return tenantId; }
    public String sessionKey() { return accountId + ":" + tenantId; }
}
