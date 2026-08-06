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

    @Column(unique = true)
    private String username;

    @Column(name = "person_id", unique = true)
    private Long personId;

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

    @Column
    private String externalProvider;

    @Column
    private String externalSubject;

    @Column
    private LocalDateTime lastLoginAt;

//    @Column(length = 255)
//    private String pictureUrl;


//    @Column(length = 4)
//    @JsonIgnore
//    private String otpCode;
//
//    @JsonIgnore
//    private LocalDateTime otpExpiresAt;
}
