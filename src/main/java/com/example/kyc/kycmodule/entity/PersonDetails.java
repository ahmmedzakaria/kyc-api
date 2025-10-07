package com.example.kyc.kycmodule.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "person_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "person_id", nullable = false, unique = true)
    private Person person;

    // Family Info
    private String fatherName;
    private String fatherMobileNumber;
    private String motherName;
    private String motherMobileNumber;
    private String emergencyContactPerson;
    private String emergencyContactPersonRelation;
    private String emergencyContactNumber;

    // Education Info
    private String educationLevel;
    private String institutionName;
    private Integer passingYear;

    // Address Info
    private UUID currentLocationId;
    private String currentLocationType;
    private String currentAddress;

    private UUID permanentLocationId;
    private String permanentLocationType;
    private String permanentAddress;

    // Audit Info
    private LocalDateTime createdAt = LocalDateTime.now();
    private Long createdBy;
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Long updatedBy;
}

