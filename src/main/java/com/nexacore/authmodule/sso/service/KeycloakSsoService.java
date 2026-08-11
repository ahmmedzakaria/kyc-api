package com.nexacore.authmodule.sso.service;

import com.nexacore.authmodule.security.config.KeycloakProperties;
import com.nexacore.authmodule.core.entity.AuthRole;
import com.nexacore.authmodule.core.entity.AuthUser;
import com.nexacore.authmodule.core.repository.RoleRepository;
import com.nexacore.authmodule.core.repository.UserRepository;
import com.nexacore.authmodule.core.service.UsernameNormalizer;
import com.nexacore.authmodule.sso.dto.SsoUserProfileDto;
import com.nexacore.gatewaymodule.person.dto.PersonSummaryDto;
import com.nexacore.gatewaymodule.person.service.interfaces.PersonModuleGateway;
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
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeycloakSsoService {

    private static final String PROVIDER = "KEYCLOAK";

    private final KeycloakProperties keycloakProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PersonModuleGateway personModuleGateway;
    private final UsernameNormalizer usernameNormalizer;

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
                claimAsLong(jwt, "nexacore_person_id"),
                username,
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
        );
    }

    @Transactional
    public AuthUser syncUser(SsoUserProfileDto profile, Long tenantId) {
        if (tenantId == null) throw new JwtException("SSO_TENANT_CONTEXT_REQUIRED");
        Optional<AuthUser> existingUser = profile.personId() == null
                ? Optional.empty()
                : userRepository.findByTenantIdAndPersonId(tenantId, profile.personId());

        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByTenantIdAndExternalProviderAndExternalSubject(tenantId, PROVIDER, profile.subject());
        }

        if (existingUser.isEmpty() && StringUtils.hasText(profile.username())) {
            existingUser = userRepository.findByTenantIdAndNormalizedUsername(tenantId, usernameNormalizer.normalize(profile.username()));
        }

        if (existingUser.isEmpty() && !keycloakProperties.isSyncUser()) {
            throw new JwtException("SSO_USER_NOT_MAPPED");
        }

        Long personId = resolvePersonId(profile);
        if (personId == null) {
            throw new JwtException("SSO_PERSON_NOT_MAPPED");
        }
        if (existingUser.isEmpty()) {
            existingUser = userRepository.findByTenantIdAndPersonId(tenantId, personId);
        }

        AuthUser user = existingUser.orElseGet(AuthUser::new);
        String username = resolveUniqueUsername(profile.username(), personId, tenantId);
        PersonSummaryDto person = personModuleGateway.promotePersonToUser(
                personId,
                username,
                profile.email(),
                profile.firstName(),
                profile.lastName()
        );
        user.setPersonId(personId);
        user.setTenantId(tenantId);
        user.setUsername(person.getUsername());
        user.setNormalizedUsername(usernameNormalizer.normalize(person.getUsername()));
        user.setExternalProvider(PROVIDER);
        user.setExternalSubject(profile.subject());
        user.setEnabled(true);
        user.setLastLoginAt(LocalDateTime.now());

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }

        if (user.getRoles().isEmpty() && StringUtils.hasText(keycloakProperties.getDefaultRole())) {
            AuthRole defaultRole = roleRepository.findByName(keycloakProperties.getDefaultRole())
                    .orElseThrow(() -> new JwtException("DEFAULT_ROLE_NOT_FOUND"));
            user.getRoles().add(defaultRole);
        }

        return userRepository.save(user);
    }

    private String resolveUniqueUsername(String requestedUsername, Long personId, Long tenantId) {
        String baseUsername = normalizeUsername(requestedUsername);
        String candidate = baseUsername;
        int suffix = 1;

        while (true) {
            Optional<AuthUser> existing = userRepository.findByTenantIdAndNormalizedUsername(
                    tenantId, usernameNormalizer.normalize(candidate));
            Optional<PersonSummaryDto> existingPerson = Optional.empty();
            boolean authUsernameAvailable = existing.isEmpty() || Objects.equals(existing.get().getPersonId(), personId);
            boolean personUsernameAvailable = existingPerson.isEmpty() || Objects.equals(existingPerson.get().getId(), personId);
            if (authUsernameAvailable && personUsernameAvailable) {
                return candidate;
            }
            candidate = baseUsername + suffix++;
        }
    }

    private String normalizeUsername(String username) {
        String normalized = username == null ? "" : username.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("^[_\\.\\-]+|[_\\.\\-]+$", "");
        return StringUtils.hasText(normalized) ? normalized : "user";
    }

    private Long resolvePersonId(SsoUserProfileDto profile) {
        if (profile.personId() != null && personModuleGateway.existsById(profile.personId())) {
            return profile.personId();
        }
        if (StringUtils.hasText(profile.username())) {
            Optional<PersonSummaryDto> person = personModuleGateway.findSummaryByUsername(profile.username());
            if (person.isPresent()) {
                return person.get().getId();
            }
        }
        if (StringUtils.hasText(profile.email())) {
            return personModuleGateway.findSummaryByEmail(profile.email())
                    .map(PersonSummaryDto::getId)
                    .orElse(null);
        }
        return null;
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
        List<String> allowedClientIds = keycloakProperties.getAllowedClientIds();

        if (!audience.contains(requiredAudience)
                && !Objects.equals(authorizedParty, requiredAudience)
                && !Objects.equals(authorizedParty, clientId)
                && (allowedClientIds == null || !allowedClientIds.contains(authorizedParty))) {
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

    private Long claimAsLong(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
