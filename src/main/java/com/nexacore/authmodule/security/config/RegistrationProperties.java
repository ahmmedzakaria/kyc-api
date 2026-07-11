package com.nexacore.authmodule.security.config;

import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    private RegistrationMode mode = RegistrationMode.APPROVAL_REQUIRED;
    private Set<RegistrationCredentialModel> credentialModels = new LinkedHashSet<>(List.of(
            RegistrationCredentialModel.EMAIL_PASSWORD,
            RegistrationCredentialModel.MOBILE_PASSWORD,
            RegistrationCredentialModel.ENTERPRISE_SSO
    ));
    private Map<String, ClientRegistrationPolicy> clients = new LinkedHashMap<>();
    private UserActivationMode activationMode = UserActivationMode.APPROVAL_REQUIRED;
    private boolean requiresExistingPerson = true;
    private List<String> allowedPersonTypes = new ArrayList<>();
    private List<String> requiredFields = new ArrayList<>();
    private List<String> requiredDocuments = new ArrayList<>();

    public RegistrationMode getMode() {
        return mode;
    }

    public void setMode(RegistrationMode mode) {
        this.mode = mode;
    }

    public Set<RegistrationCredentialModel> getCredentialModels() {
        return credentialModels;
    }

    public Set<RegistrationCredentialModel> getCredentialModels(String clientCode) {
        ClientRegistrationPolicy clientPolicy = resolveClientPolicy(clientCode);
        if (clientPolicy != null && clientPolicy.getCredentialModels() != null && !clientPolicy.getCredentialModels().isEmpty()) {
            return clientPolicy.getCredentialModels();
        }
        return credentialModels;
    }

    public void setCredentialModels(Set<RegistrationCredentialModel> credentialModels) {
        this.credentialModels = credentialModels;
    }

    public Map<String, ClientRegistrationPolicy> getClients() {
        return clients;
    }

    public void setClients(Map<String, ClientRegistrationPolicy> clients) {
        this.clients = clients;
    }

    public UserActivationMode getActivationMode() {
        return activationMode;
    }

    public void setActivationMode(UserActivationMode activationMode) {
        this.activationMode = activationMode;
    }

    public boolean isRequiresExistingPerson() {
        return requiresExistingPerson;
    }

    public void setRequiresExistingPerson(boolean requiresExistingPerson) {
        this.requiresExistingPerson = requiresExistingPerson;
    }

    public List<String> getAllowedPersonTypes() {
        return allowedPersonTypes;
    }

    public void setAllowedPersonTypes(List<String> allowedPersonTypes) {
        this.allowedPersonTypes = allowedPersonTypes;
    }

    public List<String> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(List<String> requiredFields) {
        this.requiredFields = requiredFields;
    }

    public List<String> getRequiredDocuments() {
        return requiredDocuments;
    }

    public void setRequiredDocuments(List<String> requiredDocuments) {
        this.requiredDocuments = requiredDocuments;
    }

    private ClientRegistrationPolicy resolveClientPolicy(String clientCode) {
        if (clientCode == null || clientCode.isBlank() || clients == null || clients.isEmpty()) {
            return null;
        }
        ClientRegistrationPolicy policy = clients.get(clientCode);
        if (policy != null) {
            return policy;
        }
        return clients.get(clientCode.toLowerCase());
    }

    public static class ClientRegistrationPolicy {
        private Set<RegistrationCredentialModel> credentialModels = new LinkedHashSet<>();

        public Set<RegistrationCredentialModel> getCredentialModels() {
            return credentialModels;
        }

        public void setCredentialModels(Set<RegistrationCredentialModel> credentialModels) {
            this.credentialModels = credentialModels;
        }
    }
}
