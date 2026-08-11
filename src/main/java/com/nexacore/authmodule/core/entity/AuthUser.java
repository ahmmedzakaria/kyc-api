package com.nexacore.authmodule.core.entity;

import com.nexacore.commonmodule.dto.ActionInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "auth_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "normalized_username", nullable = false, length = 150)
    private String normalizedUsername;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<AuthUserScopeAssignment> scopeAssignments = new HashSet<>();

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "auth_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<AuthRole> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled;

    @Builder.Default
    @Column(nullable = false)
    private boolean locked = false;

    @Column
    private String externalProvider;

    @Column
    private String externalSubject;

    @Column
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "credentials_expire_at")
    private LocalDateTime credentialsExpireAt;

    @Builder.Default
    @Column(name = "created_by", nullable = false)
    private Long createdBy = 0L;

    @Builder.Default
    @Column(name = "updated_by", nullable = false)
    private Long updatedBy = 0L;

//    @Column(length = 255)
//    private String pictureUrl;


//    @Column(length = 4)
//    @JsonIgnore
//    private String otpCode;
//
//    @JsonIgnore
//    private LocalDateTime otpExpiresAt;
}
