package com.nexacore.appconfigmodule.security;

import com.nexacore.authmodule.enums.AuthenticationMode;
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
