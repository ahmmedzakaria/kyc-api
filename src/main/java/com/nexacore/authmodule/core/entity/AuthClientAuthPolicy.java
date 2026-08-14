package com.nexacore.authmodule.core.entity;

import com.nexacore.authmodule.core.enums.LoginIdentifierType;
import com.nexacore.authmodule.core.enums.LoginMethod;
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
@Table(name = "auth_client_auth_policy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClientAuthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id")
    private Long tenantId;

    @Column(name = "client_code", nullable = false, length = 100)
    private String clientCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_method", nullable = false, length = 50)
    private LoginMethod loginMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_identifier_type", length = 50)
    private LoginIdentifierType loginIdentifierType;

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
