package com.nexacore.authmodule.security.config;

import com.nexacore.authmodule.core.enums.AuthenticationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthenticationProperties {

    private AuthenticationMode mode = AuthenticationMode.LOCAL;

    public AuthenticationMode getMode() {
        return mode;
    }

    public void setMode(AuthenticationMode mode) {
        this.mode = mode;
    }

    public boolean isSsoMode() {
        return AuthenticationMode.SSO.equals(mode);
    }
}
