package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientCredentialRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private final ClientApplicationRepository clientApplicationRepository;
    private final ClientCredentialRepository clientCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${access-control.credential-usage-write-interval:PT5M}")
    private Duration usageWriteInterval;

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public Optional<SysPrivClientApplication> resolveActiveClient(String clientCode) {
        if (clientCode == null || clientCode.isBlank()) {
            return Optional.empty();
        }
        return clientApplicationRepository.findByClientCode(clientCode.trim())
                .filter(application -> application.getStatus() == ClientApplicationStatus.ACTIVE);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public Optional<SysPrivClientApplication> validateApiKey(String clientCode, String apiKey) {
        if (clientCode == null || clientCode.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        Optional<SysPrivClientApplication> application = resolveActiveClient(clientCode);
        if (application.isEmpty()) {
            return Optional.empty();
        }

        return clientCredentialRepository.findByClientApplicationIdAndActiveTrue(application.get().getId()).stream()
                .filter(credential -> credential.getExpiresAt() == null || credential.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(credential -> credential.getApiKeyHash() != null && passwordEncoder.matches(apiKey, credential.getApiKeyHash()))
                .findFirst()
                .map(credential -> {
                    recordUsageIfDue(credential.getId(), credential.getLastUsedAt(), LocalDateTime.now());
                    return application.get();
                });
    }

    private void recordUsageIfDue(Long credentialId, LocalDateTime lastUsedAt, LocalDateTime usedAt) {
        Duration interval = usageWriteInterval == null ? Duration.ofMinutes(5) : usageWriteInterval;
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalStateException("access-control.credential-usage-write-interval must be positive");
        }
        LocalDateTime cutoff = usedAt.minus(interval);
        if (lastUsedAt == null || lastUsedAt.isBefore(cutoff)) {
            clientCredentialRepository.updateLastUsedAtIfBefore(credentialId, usedAt, cutoff);
        }
    }
}
