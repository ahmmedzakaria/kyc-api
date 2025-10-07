package com.example.kyc.kycmodule.dto;

import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonDetailsDto {
    private Long id;
    private Long personId;
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

