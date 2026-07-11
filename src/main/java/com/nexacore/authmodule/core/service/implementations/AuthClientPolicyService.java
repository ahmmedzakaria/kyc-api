package com.nexacore.authmodule.core.service.implementations;

import com.nexacore.authmodule.core.entity.AuthClientAuthPolicy;
import com.nexacore.authmodule.core.entity.AuthClientRegistrationPolicy;
import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import com.nexacore.authmodule.core.repository.AuthClientAuthPolicyRepository;
import com.nexacore.authmodule.core.repository.AuthClientRegistrationPolicyRepository;
import com.nexacore.authmodule.security.config.AuthenticationProperties;
import com.nexacore.authmodule.security.config.RegistrationProperties;
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
    private final AuthClientRegistrationPolicyRepository registrationPolicyRepository;
    private final AuthenticationProperties authenticationProperties;
    private final RegistrationProperties registrationProperties;

    @Transactional(transactionManager = "authTransactionManager", readOnly = true)
    public Set<LoginMethod> resolveLoginMethods(String clientCode) {
        List<AuthClientAuthPolicy> dbPolicies = loadAuthPolicies(clientCode);
        Set<LoginMethod> dbMethods = dbPolicies.stream()
                .map(AuthClientAuthPolicy::getLoginMethod)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!dbMethods.isEmpty()) {
            return dbMethods;
        }
        return new LinkedHashSet<>(authenticationProperties.getLoginMethods(clientCode));
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
        if (StringUtils.hasText(clientCode)) {
            Set<RegistrationCredentialModel> dbModels = registrationPolicyRepository
                    .findByClientCodeIgnoreCaseAndEnabledTrue(clientCode)
                    .stream()
                    .map(AuthClientRegistrationPolicy::getRegistrationCredentialModel)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!dbModels.isEmpty()) {
                return dbModels;
            }
        }
        return new LinkedHashSet<>(registrationProperties.getCredentialModels(clientCode));
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
}
