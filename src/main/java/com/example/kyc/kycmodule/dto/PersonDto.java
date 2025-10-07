package com.example.kyc.kycmodule.dto;

import lombok.*;
import java.time.LocalDate;

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
    private String photoUrl;
    private String bloodGrop;
    private Boolean emailVerified;
    private Boolean mobileVerified;
}

