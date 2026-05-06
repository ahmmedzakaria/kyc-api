package com.nexacore.authmodule.service.implementations;

import com.nexacore.appconfigmodule.security.KeycloakProperties;
import com.nexacore.authmodule.dto.SsoUserProfileDto;
import com.nexacore.authmodule.entity.Role;
import com.nexacore.authmodule.entity.User;
import com.nexacore.authmodule.repository.RoleRepository;
import com.nexacore.authmodule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeycloakSsoService {

    private static final String PROVIDER = "KEYCLOAK";

    private final KeycloakProperties keycloakProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public SsoUserProfileDto verifyToken(String accessToken) {
        JwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(keycloakProperties.getJwkSetUri()).build();
        Jwt jwt = decoder.decode(accessToken);

        validateIssuer(jwt);
        validateAudience(jwt);

        String subject = jwt.getSubject();
        String username = firstNonBlank(
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email"),
                subject
        );

        return new SsoUserProfileDto(
                subject,
                username,
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
        );
    }

    @Transactional
    public User syncUser(SsoUserProfileDto profile) {
        Optional<User> existingUser = userRepository
                .findByExternalProviderAndExternalSubject(PROVIDER, profile.subject());

        if (existingUser.isEmpty() && StringUtils.hasText(profile.email())) {
            existingUser = userRepository.findByEmail(profile.email());
        }

        if (existingUser.isEmpty() && StringUtils.hasText(profile.username())) {
            existingUser = userRepository.findByUsername(profile.username());
        }

        if (existingUser.isEmpty() && !keycloakProperties.isSyncUser()) {
            throw new JwtException("SSO_USER_NOT_MAPPED");
        }

        User user = existingUser.orElseGet(User::new);
        user.setUsername(profile.username());
        user.setEmail(profile.email());
        user.setEmailVerified(profile.emailVerified());
        user.setExternalProvider(PROVIDER);
        user.setExternalSubject(profile.subject());
        user.setEnabled(true);
        user.setLastLoginAt(LocalDateTime.now());

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        if (user.getRoles().isEmpty() && StringUtils.hasText(keycloakProperties.getDefaultRole())) {
            Role defaultRole = roleRepository.findByName(keycloakProperties.getDefaultRole())
                    .orElseThrow(() -> new JwtException("DEFAULT_ROLE_NOT_FOUND"));
            user.getRoles().add(defaultRole);
        }

        return userRepository.save(user);
    }

    private void validateIssuer(Jwt jwt) {
        String expectedIssuer = keycloakProperties.getIssuerUri();
        String actualIssuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();

        if (StringUtils.hasText(expectedIssuer) && !Objects.equals(expectedIssuer, actualIssuer)) {
            throw new JwtException("Invalid Keycloak token issuer");
        }
    }

    private void validateAudience(Jwt jwt) {
        String requiredAudience = keycloakProperties.getRequiredAudience();
        if (!StringUtils.hasText(requiredAudience)) {
            return;
        }

        List<String> audience = jwt.getAudience();
        String authorizedParty = jwt.getClaimAsString("azp");
        String clientId = keycloakProperties.getClientId();

        if (!audience.contains(requiredAudience)
                && !Objects.equals(authorizedParty, requiredAudience)
                && !Objects.equals(authorizedParty, clientId)) {
            throw new JwtException("Invalid Keycloak token audience");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
