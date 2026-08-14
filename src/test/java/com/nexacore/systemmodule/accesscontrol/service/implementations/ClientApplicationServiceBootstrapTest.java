package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationType;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientCredentialRepository;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import com.nexacore.systemmodule.accesscontrol.security.ClientIpPolicy;
import com.nexacore.systemmodule.accesscontrol.security.ClientOriginPolicy;
import com.nexacore.systemmodule.accesscontrol.security.ClientRedirectUriPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ClientApplicationServiceBootstrapTest {
    @Test
    void existingClientConfigurationIsNotOverwrittenByBootstrap() {
        ClientApplicationRepository repository = mock(ClientApplicationRepository.class);
        AuthModuleGateway auth = mock(AuthModuleGateway.class);
        SysAccClientApplication existing = SysAccClientApplication.builder()
                .id(1L).clientCode("WEB").clientName("Managed Web")
                .clientType(ClientApplicationType.WEB).status(ClientApplicationStatus.ACTIVE)
                .allowedOrigins("http://eximbank.localhost:5300")
                .oauthClientId("managed-oauth-client")
                .allowedRedirectUris("http://eximbank.localhost:5300/sso/callback")
                .allowedLogoutRedirectUris("http://eximbank.localhost:5300/login")
                .build();
        when(repository.findByClientCode("WEB")).thenReturn(Optional.of(existing));
        ClientApplicationServiceImpl service = new ClientApplicationServiceImpl(repository,
                mock(ClientCredentialRepository.class), auth, mock(PasswordEncoder.class),
                mock(ClientOriginPolicy.class), mock(ClientRedirectUriPolicy.class), mock(ClientIpPolicy.class),
                mock(AuthorizationDataCache.class));
        ClientApplicationRequestDto defaults = new ClientApplicationRequestDto();
        defaults.setClientCode("WEB");
        defaults.setAllowedOrigins("http://localhost:5300");

        var result = service.createIfAbsent(defaults, "system_admin");

        assertThat(result.getAllowedOrigins()).isEqualTo("http://eximbank.localhost:5300");
        assertThat(result.getOauthClientId()).isEqualTo("managed-oauth-client");
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(auth);
    }
}
