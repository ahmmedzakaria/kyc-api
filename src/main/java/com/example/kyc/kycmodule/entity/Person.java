package com.example.kyc.kycmodule.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 20)
    private String mobileNumber;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationalId;
    private String photoUrl;
    private String bloodGrop;

    private Boolean emailVerified = false;
    private Boolean mobileVerified = false;
}

