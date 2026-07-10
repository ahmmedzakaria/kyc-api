package com.nexacore.gatewaymodule.person.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSummaryDto {
    private Long id;
    private String username;
    private String mobileNumber;
    private String email;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String nationalId;
    private Boolean emailVerified;
    private Boolean mobileVerified;
    private Boolean user;
}
