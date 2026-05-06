package com.nexacore.kycmodule.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDto {
    private Long id;
    private String username;
    private String mobileNumber;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationalId;
    private String bloodGroup;
    private String photoUrl;
    private Boolean emailVerified;
    private Boolean mobileVerified;

    private String fatherName;
    private String fatherMobileNumber;
    private String motherName;
    private String motherMobileNumber;
    private String emergencyContactPerson;
    private String emergencyContactPersonRelation;
    private String emergencyContactNumber;
    private String educationLevel;
    private String institutionName;
    private Integer passingYear;
    private UUID currentLocationId;
    private String currentLocationType;
    private String currentAddress;
    private UUID permanentLocationId;
    private String permanentLocationType;
    private String permanentAddress;
}
