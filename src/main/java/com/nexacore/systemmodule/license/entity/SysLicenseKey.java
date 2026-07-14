package com.nexacore.systemmodule.license.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_license_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysLicenseKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_subscription_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private SysLicenseSubscription licenseSubscription;

    @Column(nullable = false, unique = true, length = 128)
    private String licenseKeyHash;

    @Column(nullable = false, length = 16)
    private String keyPrefix;

    @Column(length = 128)
    private String activationFingerprintHash;

    private LocalDateTime issuedAt;
    private LocalDateTime activatedAt;
    private LocalDateTime lastValidatedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime revokedAt;

    @Column(nullable = false)
    private boolean active;

    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        issuedAt = issuedAt == null ? now : issuedAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
