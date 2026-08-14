package com.nexacore.systemmodule.license.service.implementations;

import com.nexacore.systemmodule.license.dto.GeneratedLicenseKeyDto;
import com.nexacore.systemmodule.license.dto.LicenseKeyRequestDto;
import com.nexacore.systemmodule.license.dto.LicenseKeySummaryDto;
import com.nexacore.systemmodule.license.entity.SysLicenseKey;
import com.nexacore.systemmodule.license.entity.SysLicenseSubscription;
import com.nexacore.systemmodule.license.repository.LicenseKeyRepository;
import com.nexacore.systemmodule.license.repository.LicenseSubscriptionRepository;
import com.nexacore.systemmodule.license.service.interfaces.LicenseKeyService;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import com.nexacore.systemmodule.license.entity.SysLicenseAuditEvent;
import com.nexacore.systemmodule.license.repository.LicenseAuditEventRepository;

@Service
@RequiredArgsConstructor
public class LicenseKeyServiceImpl implements LicenseKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final LicenseKeyRepository licenseKeyRepository;
    private final LicenseSubscriptionRepository subscriptionRepository;
    private final DataScopeService dataScopeService;
    private final LicenseAuditEventRepository auditRepository;

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public GeneratedLicenseKeyDto generateLicenseKey(LicenseKeyRequestDto request) {
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        SysLicenseSubscription subscription = subscriptionRepository.findBySubscriptionCodeAndTenantId(
                        request.subscriptionCode(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("License subscription not found: " + request.subscriptionCode()));
        String rawKey = generateRawKey(subscription.getSubscriptionCode());
        String keyHash = sha256(rawKey);
        SysLicenseKey key = licenseKeyRepository.save(SysLicenseKey.builder()
                .licenseSubscription(subscription)
                .licenseKeyHash(keyHash)
                .keyPrefix(rawKey.substring(0, Math.min(12, rawKey.length())))
                .expiresAt(request.expiresAt())
                .active(true)
                .build());

        return GeneratedLicenseKeyDto.builder()
                .id(key.getId())
                .subscriptionCode(subscription.getSubscriptionCode())
                .rawLicenseKey(rawKey)
                .keyPrefix(key.getKeyPrefix())
                .expiresAt(key.getExpiresAt())
                .build();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public boolean activateLicenseKey(LicenseKeyRequestDto request) {
        return licenseKeyRepository.findByLicenseKeyHashAndLicenseSubscriptionTenantId(
                        sha256(request.rawLicenseKey()), dataScopeService.requireEffectiveTenant(null))
                .filter(this::isUsable)
                .map(key -> {
                    key.setActivationFingerprintHash(sha256(request.activationFingerprint()));
                    key.setActivatedAt(LocalDateTime.now());
                    key.setLastValidatedAt(LocalDateTime.now());
                    licenseKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public boolean validateLicenseKey(LicenseKeyRequestDto request) {
        return licenseKeyRepository.findByLicenseKeyHashAndLicenseSubscriptionTenantId(
                        sha256(request.rawLicenseKey()), dataScopeService.requireEffectiveTenant(null))
                .filter(this::isUsable)
                .map(key -> {
                    key.setLastValidatedAt(LocalDateTime.now());
                    licenseKeyRepository.save(key);
                    return true;
                })
                .orElse(false);
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager", readOnly = true)
    public List<LicenseKeySummaryDto> listKeys(String subscriptionCode) {
        return licenseKeyRepository
                .findByLicenseSubscriptionSubscriptionCodeAndLicenseSubscriptionTenantIdOrderByIssuedAtDesc(
                        subscriptionCode, dataScopeService.requireEffectiveTenant(null))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(transactionManager = "systemTransactionManager")
    public LicenseKeySummaryDto revoke(Long keyId, String reason, Long actorUserId) {
        Long tenantId = dataScopeService.requireEffectiveTenant(null);
        SysLicenseKey key = licenseKeyRepository.findByIdAndLicenseSubscriptionTenantId(keyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("License key not found"));
        if (key.getRevokedAt() == null) {
            key.setActive(false);
            key.setRevokedAt(LocalDateTime.now());
            licenseKeyRepository.saveAndFlush(key);
            SysLicenseSubscription subscription = key.getLicenseSubscription();
            auditRepository.save(SysLicenseAuditEvent.builder().licenseSubscription(subscription)
                    .eventType("KEY_REVOKED").eventMessageCode("system.license.key.revoked")
                    .actorUserId(actorUserId).tenantId(subscription.getTenantId()).businessId(subscription.getBusinessId())
                    .clientApplicationId(subscription.getClientApplication() == null ? null : subscription.getClientApplication().getId())
                    .safeContextJson("{\"keyId\":" + keyId + ",\"reason\":\"" + safe(reason) + "\"}")
                    .createdBy(actorUserId).updatedBy(actorUserId).build());
        }
        return toSummary(key);
    }

    private String safe(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private LicenseKeySummaryDto toSummary(SysLicenseKey key) {
        return LicenseKeySummaryDto.builder()
                .id(key.getId())
                .subscriptionCode(key.getLicenseSubscription().getSubscriptionCode())
                .keyPrefix(key.getKeyPrefix())
                .issuedAt(key.getIssuedAt())
                .activatedAt(key.getActivatedAt())
                .lastValidatedAt(key.getLastValidatedAt())
                .expiresAt(key.getExpiresAt())
                .revokedAt(key.getRevokedAt())
                .active(key.isActive())
                .build();
    }

    private boolean isUsable(SysLicenseKey key) {
        LocalDateTime now = LocalDateTime.now();
        return key.isActive()
                && key.getRevokedAt() == null
                && (key.getExpiresAt() == null || !key.getExpiresAt().isBefore(now));
    }

    private String generateRawKey(String subscriptionCode) {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return "NC-" + subscriptionCode + "-" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private String sha256(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("License key value is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
