package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientCredentialRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientCredentialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClientCredentialServiceImpl implements ClientCredentialService {

    private final ClientApplicationRepository clientApplicationRepository;
    private final ClientCredentialRepository clientCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public Optional<SysPrivClientApplication> validateApiKey(String clientCode, String apiKey) {
        if (clientCode == null || clientCode.isBlank() || apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }

        Optional<SysPrivClientApplication> application = clientApplicationRepository.findByClientCode(clientCode.trim());
        if (application.isEmpty() || application.get().getStatus() != ClientApplicationStatus.ACTIVE) {
            return Optional.empty();
        }

        return clientCredentialRepository.findByClientApplicationIdAndActiveTrue(application.get().getId()).stream()
                .filter(credential -> credential.getExpiresAt() == null || credential.getExpiresAt().isAfter(LocalDateTime.now()))
                .filter(credential -> credential.getApiKeyHash() != null && passwordEncoder.matches(apiKey, credential.getApiKeyHash()))
                .findFirst()
                .map(credential -> {
                    credential.setLastUsedAt(LocalDateTime.now());
                    clientCredentialRepository.save(credential);
                    return application.get();
                });
    }
}
