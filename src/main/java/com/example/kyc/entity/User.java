package com.example.kyc.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String username;

    @Column
    private String name;

    @Column
    private String email;

    @Column
    private boolean emailVerified;

    @Column
    private String mobile;

    @Column
    private boolean mobileVerified;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @Column(length = 255)
    private String pictureUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    @JsonIgnore
    private Role role;

    
    @Column(nullable = false)
    private boolean active;

    @Column(length = 4)
    @JsonIgnore
    private String otpCode;

    @JsonIgnore
    private LocalDateTime otpExpiresAt;
}
