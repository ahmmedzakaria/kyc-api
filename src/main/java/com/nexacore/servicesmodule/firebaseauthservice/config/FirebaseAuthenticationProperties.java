package com.nexacore.servicesmodule.firebaseauthservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "firebase.auth")
public class FirebaseAuthenticationProperties {

    private boolean enabled = false;

    private String projectId;

    private String serviceAccountPath;

    private boolean useApplicationDefaultCredentials = true;

    private String phoneSignInProvider = "phone";
}
