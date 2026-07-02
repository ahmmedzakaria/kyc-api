package com.nexacore.servicesmodule.firebaseauthservice.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.auth.enabled", havingValue = "true")
public class FirebaseAdminConfig {

    private final FirebaseAuthenticationProperties properties;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(resolveCredentials());

        if (StringUtils.hasText(properties.getProjectId())) {
            optionsBuilder.setProjectId(properties.getProjectId().trim());
        }

        return FirebaseApp.initializeApp(optionsBuilder.build());
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        if (StringUtils.hasText(properties.getServiceAccountPath())) {
            try (InputStream serviceAccount = new FileInputStream(properties.getServiceAccountPath().trim())) {
                return GoogleCredentials.fromStream(serviceAccount);
            }
        }

        if (properties.isUseApplicationDefaultCredentials()) {
            return GoogleCredentials.getApplicationDefault();
        }

        throw new IllegalStateException("Firebase service account path is not configured");
    }
}
