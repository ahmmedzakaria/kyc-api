package com.nexacore.appconfigmodule.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String issuerUri = "http://localhost:9200/realms/kyc";
    private String jwkSetUri = "http://localhost:9200/realms/kyc/protocol/openid-connect/certs";
    private String clientId = "nexacore-client";
    private List<String> allowedClientIds = new ArrayList<>(List.of("nexacore-client", "privilege-frontend"));
    private String requiredAudience = "nexacore";
    private boolean syncUser = true;
    private String defaultRole = "ROLE_KYC_OPERATOR";

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getAllowedClientIds() {
        return allowedClientIds;
    }

    public void setAllowedClientIds(List<String> allowedClientIds) {
        this.allowedClientIds = allowedClientIds;
    }

    public String getRequiredAudience() {
        return requiredAudience;
    }

    public void setRequiredAudience(String requiredAudience) {
        this.requiredAudience = requiredAudience;
    }

    public boolean isSyncUser() {
        return syncUser;
    }

    public void setSyncUser(boolean syncUser) {
        this.syncUser = syncUser;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
}
