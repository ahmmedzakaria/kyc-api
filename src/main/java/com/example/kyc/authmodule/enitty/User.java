package com.example.kyc.authmodule.enitty;

import com.example.kyc.commonmodule.dto.ActionInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends ActionInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String username;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled;

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

    @Column(length = 255)
    private String pictureUrl;


//    @Column(length = 4)
//    @JsonIgnore
//    private String otpCode;
//
//    @JsonIgnore
//    private LocalDateTime otpExpiresAt;
}
