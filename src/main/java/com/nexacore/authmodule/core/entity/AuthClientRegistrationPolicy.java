package com.nexacore.authmodule.core.entity;

import com.nexacore.authmodule.core.enums.RegistrationCredentialModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_client_registration_policy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClientRegistrationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_code", nullable = false, length = 100)
    private String clientCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_credential_model", nullable = false, length = 80)
    private RegistrationCredentialModel registrationCredentialModel;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
