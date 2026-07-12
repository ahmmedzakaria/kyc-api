package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthClientPolicyService {

    private final AuthClientAuthPolicyRepository authPolicyRepository;
    private final AuthenticationProperties authenticationProperties;

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Set<LoginMethod> resolveLoginMethods(String clientCode) {
        List<AuthClientAuthPolicy> dbPolicies = loadAuthPolicies(clientCode);
        Set<LoginMethod> dbMethods = dbPolicies.stream()
                .map(AuthClientAuthPolicy::getLoginMethod)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!dbMethods.isEmpty()) {
            return firstLoginMethodOnly(dbMethods);
        }
        return firstLoginMethodOnly(authenticationProperties.getLoginMethods(clientCode));
    }

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Set<LoginIdentifierType> resolveLoginIdentifierTypes(String clientCode) {
        List<AuthClientAuthPolicy> dbPolicies = loadAuthPolicies(clientCode);
        Set<LoginIdentifierType> dbIdentifiers = dbPolicies.stream()
                .map(AuthClientAuthPolicy::getLoginIdentifierType)
                .filter(identifier -> identifier != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!dbIdentifiers.isEmpty()) {
            return dbIdentifiers;
        }
        return new LinkedHashSet<>(authenticationProperties.getLoginIdentifiers(clientCode));
    }

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Set<RegistrationCredentialModel> resolveRegistrationCredentialModels(String clientCode) {
        List<AuthClientAuthPolicy> dbPolicies = loadAuthPolicies(clientCode);
        if (!dbPolicies.isEmpty()) {
            return mapRegistrationCredentialModels(
                    dbPolicies.stream()
                            .map(AuthClientAuthPolicy::getLoginMethod)
                            .collect(Collectors.toCollection(LinkedHashSet::new)),
                    dbPolicies.stream()
                            .map(AuthClientAuthPolicy::getLoginIdentifierType)
                            .filter(identifier -> identifier != null)
                            .collect(Collectors.toCollection(LinkedHashSet::new))
            );
        }

        return mapRegistrationCredentialModels(
                authenticationProperties.getLoginMethods(clientCode),
                authenticationProperties.getLoginIdentifiers(clientCode)
        );
    }

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public boolean isLoginMethodEnabled(LoginMethod loginMethod, String clientCode) {
        return resolveLoginMethods(clientCode).contains(loginMethod);
    }

    private List<AuthClientAuthPolicy> loadAuthPolicies(String clientCode) {
        if (!StringUtils.hasText(clientCode)) {
            return List.of();
        }
        return authPolicyRepository.findByClientCodeIgnoreCaseAndEnabledTrue(clientCode);
    }

    private Set<RegistrationCredentialModel> mapRegistrationCredentialModels(Set<LoginMethod> loginMethods,
                                                                             Set<LoginIdentifierType> loginIdentifierTypes) {
        Set<RegistrationCredentialModel> credentialModels = new LinkedHashSet<>();
        Set<LoginIdentifierType> identifiers = loginIdentifierTypes == null ? Set.of() : loginIdentifierTypes;

        for (LoginMethod loginMethod : firstLoginMethodOnly(loginMethods)) {
            switch (loginMethod) {
                case PASSWORD -> {
                    if (identifiers.contains(LoginIdentifierType.MOBILE)) {
                        credentialModels.add(RegistrationCredentialModel.MOBILE_PASSWORD);
                    }
                    if (identifiers.isEmpty()
                            || identifiers.contains(LoginIdentifierType.EMAIL)
                            || identifiers.contains(LoginIdentifierType.USERNAME)) {
                        credentialModels.add(RegistrationCredentialModel.EMAIL_PASSWORD);
                    }
                }
                case OTP -> credentialModels.add(RegistrationCredentialModel.MOBILE_OTP);
                case MAGIC_LINK -> credentialModels.add(RegistrationCredentialModel.MAGIC_LINK);
                case OAUTH -> credentialModels.add(RegistrationCredentialModel.SOCIAL_OAUTH);
                case SSO -> credentialModels.add(RegistrationCredentialModel.ENTERPRISE_SSO);
                case MFA -> credentialModels.add(RegistrationCredentialModel.MFA_ENROLLMENT);
                case BIOMETRIC -> credentialModels.add(RegistrationCredentialModel.BIOMETRIC_ENROLLMENT);
                case PASSKEY -> credentialModels.add(RegistrationCredentialModel.PASSKEY_ENROLLMENT);
            }
        }

        return credentialModels;
    }

    private Set<LoginMethod> firstLoginMethodOnly(Set<LoginMethod> loginMethods) {
        if (loginMethods == null || loginMethods.isEmpty()) {
            return new LinkedHashSet<>(List.of(LoginMethod.PASSWORD));
        }
        return loginMethods.stream()
                .limit(1)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
