package com.nexacore.servicesmodule.firebaseauthservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebasePhoneVerificationResponse {

    private boolean verified;

    private String uid;

    private String phoneNumber;

    private String signInProvider;

    private String issuer;

    private List<String> audience;

    private Instant issuedAt;

    private Instant expiresAt;

    private Map<String, Object> firebaseClaims;
}

