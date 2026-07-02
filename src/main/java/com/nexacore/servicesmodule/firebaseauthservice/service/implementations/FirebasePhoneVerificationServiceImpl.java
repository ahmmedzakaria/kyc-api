package com.nexacore.servicesmodule.firebaseauthservice.service.implementations;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nexacore.servicesmodule.firebaseauthservice.config.FirebaseAuthenticationProperties;
import com.nexacore.servicesmodule.firebaseauthservice.dto.FirebasePhoneVerificationRequest;
import com.nexacore.servicesmodule.firebaseauthservice.dto.FirebasePhoneVerificationResponse;
import com.nexacore.servicesmodule.firebaseauthservice.service.interfaces.FirebasePhoneVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "firebase.auth.enabled", havingValue = "true")
public class FirebasePhoneVerificationServiceImpl implements FirebasePhoneVerificationService {

    private final FirebaseAuthenticationProperties properties;
    private final FirebaseAuth firebaseAuth;

    @Override
    public FirebasePhoneVerificationResponse verifyPhoneToken(String idToken) {
        return verifyPhoneToken(FirebasePhoneVerificationRequest.builder()
                .idToken(idToken)
                .build());
    }

    @Override
    public FirebasePhoneVerificationResponse verifyPhoneToken(FirebasePhoneVerificationRequest request) {
        validateRequest(request);

        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(request.getIdToken().trim());
            Map<String, Object> claims = token.getClaims();
            String phoneNumber = claimAsString(claims, "phone_number");
            String signInProvider = resolveSignInProvider(claims);

            if (!StringUtils.hasText(phoneNumber)) {
                throw new IllegalStateException("Firebase token does not contain phone_number claim");
            }

            if (!Objects.equals(properties.getPhoneSignInProvider(), signInProvider)) {
                throw new IllegalStateException("Firebase token was not issued by phone sign-in provider");
            }

            if (StringUtils.hasText(request.getExpectedPhoneNumber())
                    && !normalizePhoneNumber(request.getExpectedPhoneNumber()).equals(normalizePhoneNumber(phoneNumber))) {
                throw new IllegalStateException("Firebase token phone number does not match expected phone number");
            }

            return FirebasePhoneVerificationResponse.builder()
                    .verified(true)
                    .uid(token.getUid())
                    .phoneNumber(phoneNumber)
                    .signInProvider(signInProvider)
                    .issuer(claimAsString(claims, "iss"))
                    .audience(claimAsList(claims, "aud"))
                    .issuedAt(claimAsInstant(claims, "iat"))
                    .expiresAt(claimAsInstant(claims, "exp"))
                    .firebaseClaims(claimAsMap(claims, "firebase"))
                    .build();
        } catch (FirebaseAuthException e) {
            throw new IllegalStateException("Firebase ID token verification failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> claimAsMap(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return null;
    }

    private List<String> claimAsList(Map<String, Object> claims, String name) {
        String value = claimAsString(claims, name);
        return StringUtils.hasText(value) ? List.of(value) : List.of();
    }

    private String resolveSignInProvider(Map<String, Object> claims) {
        Map<String, Object> firebaseClaims = claimAsMap(claims, "firebase");
        if (firebaseClaims == null) {
            return null;
        }
        return claimAsString(firebaseClaims, "sign_in_provider");
    }

    private String claimAsString(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        return value == null ? null : value.toString();
    }

    private Instant claimAsInstant(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        return null;
    }

    private void validateRequest(FirebasePhoneVerificationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Firebase phone verification request must not be null");
        }
        if (!StringUtils.hasText(request.getIdToken())) {
            throw new IllegalArgumentException("Firebase idToken must not be blank");
        }
    }

    private String normalizePhoneNumber(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }
}
