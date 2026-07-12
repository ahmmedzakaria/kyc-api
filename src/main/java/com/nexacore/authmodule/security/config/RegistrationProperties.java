package com.nexacore.authmodule.security.config;

import com.nexacore.authmodule.core.enums.RegistrationMode;
import com.nexacore.authmodule.core.enums.UserActivationMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    private RegistrationMode mode = RegistrationMode.APPROVAL_REQUIRED;
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
}
