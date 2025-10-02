package com.example.kyc.kycmodule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycDto {
    private Long id;

    @NotBlank(message = "firstName is required")
    @Size(max = 255)
    private String firstName;

    @Size(max = 255)
    private String lastName;

    @NotBlank(message = "nationalId is required")
    @Size(max = 255)
    private String nationalId;

    @Size(max = 255)
    private String email;

    @Size(max = 100)
    private String phone;

    private String photoString;
}
