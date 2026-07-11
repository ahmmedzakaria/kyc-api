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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthClientPolicyServiceTest {

    private final AuthClientAuthPolicyRepository authPolicyRepository = mock(AuthClientAuthPolicyRepository.class);
    private final AuthClientRegistrationPolicyRepository registrationPolicyRepository = mock(AuthClientRegistrationPolicyRepository.class);
    private final AuthenticationProperties authenticationProperties = new AuthenticationProperties();
    private final RegistrationProperties registrationProperties = new RegistrationProperties();
    private final AuthClientPolicyService service = new AuthClientPolicyService(
            authPolicyRepository,
            registrationPolicyRepository,
            authenticationProperties,
            registrationProperties
    );

    @Test
    void resolvesLoginMethodsFromDatabaseBeforeProperties() {
        when(authPolicyRepository.findByClientCodeIgnoreCaseAndEnabledTrue("CLIENT-001"))
                .thenReturn(List.of(AuthClientAuthPolicy.builder()
                        .clientCode("CLIENT-001")
                        .loginMethod(LoginMethod.SSO)
                        .loginIdentifierType(LoginIdentifierType.USERNAME)
                        .enabled(true)
                        .build()));

        Set<LoginMethod> loginMethods = service.resolveLoginMethods("CLIENT-001");
        Set<LoginIdentifierType> identifiers = service.resolveLoginIdentifierTypes("CLIENT-001");

        assertThat(loginMethods).containsExactly(LoginMethod.SSO);
        assertThat(identifiers).containsExactly(LoginIdentifierType.USERNAME);
    }

    @Test
    void fallsBackToPropertyLoginMethodsWhenDatabaseHasNoRows() {
        when(authPolicyRepository.findByClientCodeIgnoreCaseAndEnabledTrue("CLIENT-002")).thenReturn(List.of());

        Set<LoginMethod> loginMethods = service.resolveLoginMethods("CLIENT-002");

        assertThat(loginMethods).containsExactly(LoginMethod.SSO);
    }

    @Test
    void resolvesRegistrationCredentialModelsFromDatabaseBeforeProperties() {
        when(registrationPolicyRepository.findByClientCodeIgnoreCaseAndEnabledTrue("CLIENT-001"))
                .thenReturn(List.of(AuthClientRegistrationPolicy.builder()
                        .clientCode("CLIENT-001")
                        .registrationCredentialModel(RegistrationCredentialModel.ENTERPRISE_SSO)
                        .enabled(true)
                        .build()));

        Set<RegistrationCredentialModel> credentialModels = service.resolveRegistrationCredentialModels("CLIENT-001");

        assertThat(credentialModels).containsExactly(RegistrationCredentialModel.ENTERPRISE_SSO);
    }

    @Test
    void fallsBackToPropertyRegistrationCredentialModelsWhenDatabaseHasNoRows() {
        when(registrationPolicyRepository.findByClientCodeIgnoreCaseAndEnabledTrue("CLIENT-002")).thenReturn(List.of());

        Set<RegistrationCredentialModel> credentialModels = service.resolveRegistrationCredentialModels("CLIENT-002");

        assertThat(credentialModels)
                .containsExactly(
                        RegistrationCredentialModel.EMAIL_PASSWORD,
                        RegistrationCredentialModel.MOBILE_PASSWORD,
                        RegistrationCredentialModel.ENTERPRISE_SSO
                );
    }
}
