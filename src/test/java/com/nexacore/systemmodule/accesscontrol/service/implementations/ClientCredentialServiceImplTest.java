package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientApplication;
import com.nexacore.systemmodule.accesscontrol.entity.SysAccClientCredential;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClientCredentialServiceImplTest {
    private ClientApplicationRepository applications;
    private ClientCredentialRepository credentials;
    private PasswordEncoder passwordEncoder;
    private ClientCredentialServiceImpl service;
    private SysAccClientApplication client;

    @BeforeEach
    void setUp() {
        applications = mock(ClientApplicationRepository.class);
        credentials = mock(ClientCredentialRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new ClientCredentialServiceImpl(applications, credentials, passwordEncoder);
        ReflectionTestUtils.setField(service, "usageWriteInterval", Duration.ofMinutes(5));
        client = SysAccClientApplication.builder().id(10L).clientCode("PARTNER").status(ClientApplicationStatus.ACTIVE).build();
        when(applications.findByClientCode("PARTNER")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
    }

    @Test
    void doesNotWriteWhenUsageWasRecentlyRecorded() {
        credential(LocalDateTime.now().minusMinutes(1));
        assertThat(service.validateApiKey("PARTNER", "secret")).contains(client);
        verify(credentials, never()).updateLastUsedAtIfBefore(any(), any(), any());
        verify(credentials, never()).save(any());
    }

    @Test
    void conditionallyUpdatesStaleUsageWithoutSavingManagedEntity() {
        credential(LocalDateTime.now().minusMinutes(10));
        assertThat(service.validateApiKey("PARTNER", "secret")).contains(client);
        verify(credentials).updateLastUsedAtIfBefore(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(credentials, never()).save(any());
    }

    @Test
    void firstSuccessfulUseRecordsUsage() {
        credential(null);
        assertThat(service.validateApiKey("PARTNER", "secret")).contains(client);
        verify(credentials).updateLastUsedAtIfBefore(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void invalidApiKeyNeverRecordsUsage() {
        credential(null);
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        assertThat(service.validateApiKey("PARTNER", "wrong")).isEmpty();
        verify(credentials, never()).updateLastUsedAtIfBefore(any(), any(), any());
    }

    private void credential(LocalDateTime lastUsedAt) {
        SysAccClientCredential credential = SysAccClientCredential.builder().id(20L).clientApplication(client)
                .apiKeyHash("hash").active(true).lastUsedAt(lastUsedAt).build();
        when(credentials.findByClientApplicationIdAndActiveTrue(10L)).thenReturn(List.of(credential));
    }
}
