package com.nexacore.authmodule.security.config;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthenticationProperties {

    private Set<LoginMethod> loginMethods = new LinkedHashSet<>(List.of(LoginMethod.SSO));
    private Set<LoginIdentifierType> loginIdentifiers = new LinkedHashSet<>(List.of(LoginIdentifierType.USERNAME));
    private Map<String, ClientAuthenticationPolicy> clients = new LinkedHashMap<>();
    private long sessionTimeoutSeconds = 3600;
    private boolean refreshTokenEnabled = true;
    private int maxLoginAttempts = 5;
    private String passwordPolicyCode = "DEFAULT";

    public Set<LoginMethod> getLoginMethods() {
        return loginMethods;
    }

    public Set<LoginMethod> getLoginMethods(String clientCode) {
        ClientAuthenticationPolicy clientPolicy = resolveClientPolicy(clientCode);
        if (clientPolicy != null && clientPolicy.getLoginMethods() != null && !clientPolicy.getLoginMethods().isEmpty()) {
            return clientPolicy.getLoginMethods();
        }
        return loginMethods;
    }

    public void setLoginMethods(Set<LoginMethod> loginMethods) {
        this.loginMethods = loginMethods;
    }

    public Set<LoginIdentifierType> getLoginIdentifiers() {
        return loginIdentifiers;
    }

    public Set<LoginIdentifierType> getLoginIdentifiers(String clientCode) {
        ClientAuthenticationPolicy clientPolicy = resolveClientPolicy(clientCode);
        if (clientPolicy != null && clientPolicy.getLoginIdentifiers() != null && !clientPolicy.getLoginIdentifiers().isEmpty()) {
            return clientPolicy.getLoginIdentifiers();
        }
        return loginIdentifiers;
    }

    public void setLoginIdentifiers(Set<LoginIdentifierType> loginIdentifiers) {
        this.loginIdentifiers = loginIdentifiers;
    }

    public Map<String, ClientAuthenticationPolicy> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientAuthenticationPolicy> clients) {
        this.clients = clients;
    }

    public long getSessionTimeoutSeconds() {
        return sessionTimeoutSeconds;
    }

    public void setSessionTimeoutSeconds(long sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public boolean isRefreshTokenEnabled() {
        return refreshTokenEnabled;
    }

    public void setRefreshTokenEnabled(boolean refreshTokenEnabled) {
        this.refreshTokenEnabled = refreshTokenEnabled;
    }

    public int getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(int maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public String getPasswordPolicyCode() {
        return passwordPolicyCode;
    }

    public void setPasswordPolicyCode(String passwordPolicyCode) {
        this.passwordPolicyCode = passwordPolicyCode;
    }

    public boolean isLoginMethodEnabled(LoginMethod loginMethod) {
        return loginMethods != null && loginMethods.contains(loginMethod);
    }

    public boolean isLoginMethodEnabled(LoginMethod loginMethod, String clientCode) {
        Set<LoginMethod> resolvedLoginMethods = getLoginMethods(clientCode);
        return resolvedLoginMethods != null && resolvedLoginMethods.contains(loginMethod);
    }

    private ClientAuthenticationPolicy resolveClientPolicy(String clientCode) {
        if (clientCode == null || clientCode.isBlank() || clients == null || clients.isEmpty()) {
            return null;
        }
        ClientAuthenticationPolicy policy = clients.get(clientCode);
        if (policy != null) {
            return policy;
        }
        return clients.get(clientCode.toLowerCase());
    }

    public static class ClientAuthenticationPolicy {
        private Set<LoginMethod> loginMethods = new LinkedHashSet<>();
        private Set<LoginIdentifierType> loginIdentifiers = new LinkedHashSet<>();

        public Set<LoginMethod> getLoginMethods() {
            return loginMethods;
        }

        public void setLoginMethods(Set<LoginMethod> loginMethods) {
            this.loginMethods = loginMethods;
        }

        public Set<LoginIdentifierType> getLoginIdentifiers() {
            return loginIdentifiers;
        }

        public void setLoginIdentifiers(Set<LoginIdentifierType> loginIdentifiers) {
            this.loginIdentifiers = loginIdentifiers;
        }
    }
}
