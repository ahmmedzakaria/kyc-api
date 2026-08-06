package com.nexacore.systemmodule.accesscontrol.service.implementations;

import com.nexacore.gatewaymodule.auth.service.interfaces.AuthModuleGateway;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationDto;
import com.nexacore.systemmodule.accesscontrol.dto.ClientApplicationRequestDto;
import com.nexacore.systemmodule.accesscontrol.dto.GeneratedClientCredentialDto;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientApplication;
import com.nexacore.systemmodule.accesscontrol.entity.SysPrivClientCredential;
import com.nexacore.systemmodule.accesscontrol.enums.ClientApplicationStatus;
import com.nexacore.systemmodule.accesscontrol.repository.ClientApplicationRepository;
import com.nexacore.systemmodule.accesscontrol.repository.ClientCredentialRepository;
import com.nexacore.systemmodule.accesscontrol.service.interfaces.ClientApplicationService;
import com.nexacore.systemmodule.accesscontrol.security.ClientOriginPolicy;
import com.nexacore.systemmodule.accesscontrol.security.ClientIpPolicy;
import com.nexacore.systemmodule.accesscontrol.security.AuthorizationDataCache;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientApplicationServiceImpl implements ClientApplicationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ClientApplicationRepository clientApplicationRepository;
    private final ClientCredentialRepository clientCredentialRepository;
    private final AuthModuleGateway authModuleGateway;
    private final PasswordEncoder passwordEncoder;
    private final ClientOriginPolicy clientOriginPolicy;
    private final ClientIpPolicy clientIpPolicy;
    private final AuthorizationDataCache authorizationDataCache;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public ClientApplicationDto save(ClientApplicationRequestDto requestDto, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysPrivClientApplication application = requestDto.getId() == null
                ? clientApplicationRepository.findByClientCode(requestDto.getClientCode()).orElseGet(SysPrivClientApplication::new)
                : clientApplicationRepository.findById(requestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + requestDto.getId()));

        application.setClientCode(requireText(requestDto.getClientCode(), "clientCode"));
        application.setClientName(requireText(requestDto.getClientName(), "clientName"));
        application.setClientType(requestDto.getClientType());
        application.setStatus(requestDto.getStatus() == null ? ClientApplicationStatus.ACTIVE : requestDto.getStatus());
        application.setAllowedOrigins(clientOriginPolicy.normalizeConfiguredOrigins(requestDto.getAllowedOrigins()));
        application.setAllowedIps(clientIpPolicy.normalizeConfiguredIps(requestDto.getAllowedIps()));
        if (requestDto.getRateLimitPerMinute() != null && requestDto.getRateLimitPerMinute() <= 0) {
            throw new IllegalArgumentException("rateLimitPerMinute must be positive when configured");
        }
        application.setRateLimitPerMinute(requestDto.getRateLimitPerMinute());
        application.setDescription(requestDto.getDescription());
        if (application.getId() == null) {
            application.setCreatedBy(actorId);
        }
        application.setUpdatedBy(actorId);

        ClientApplicationDto saved = ClientApplicationDto.fromEntity(clientApplicationRepository.save(application));
        authorizationDataCache.invalidateClientAfterCommit(application.getId());
        return saved;
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<ClientApplicationDto> list() {
        return clientApplicationRepository.findAll().stream()
                .map(ClientApplicationDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public SysPrivClientApplication requireClientApplication(Long clientApplicationId, String clientCode) {
        if (clientApplicationId != null) {
            return clientApplicationRepository.findById(clientApplicationId)
                    .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + clientApplicationId));
        }
        return clientApplicationRepository.findByClientCode(requireText(clientCode, "clientCode"))
                .orElseThrow(() -> new IllegalArgumentException("Client application not found: " + clientCode));
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public GeneratedClientCredentialDto rotateApiKey(Long clientApplicationId, String clientCode, String username) {
        Long actorId = authModuleGateway.getUserId(username);
        SysPrivClientApplication application = requireClientApplication(clientApplicationId, clientCode);
        clientCredentialRepository.findByClientApplicationIdAndActiveTrue(application.getId())
                .forEach(credential -> {
                    credential.setActive(false);
                    credential.setUpdatedBy(actorId);
                    clientCredentialRepository.save(credential);
                });

        String apiKey = generateSecret();
        String clientId = application.getClientCode() + "-" + generateToken(9);
        SysPrivClientCredential credential = SysPrivClientCredential.builder()
                .clientApplication(application)
                .clientId(clientId)
                .apiKeyHash(passwordEncoder.encode(apiKey))
                .active(true)
                .createdBy(actorId)
                .updatedBy(actorId)
                .build();
        clientCredentialRepository.save(credential);
        authorizationDataCache.invalidateClientAfterCommit(application.getId());

        return GeneratedClientCredentialDto.builder()
                .clientCode(application.getClientCode())
                .clientId(clientId)
                .apiKey(apiKey)
                .build();
    }

    private String generateSecret() {
        return "nxa_" + generateToken(32);
    }

    private String generateToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
