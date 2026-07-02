package com.nexacore.servicesmodule.firebaseauthservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirebasePhoneVerificationRequest {

    @NotBlank(message = "idToken can not be blank")
    private String idToken;

    private String expectedPhoneNumber;
}

