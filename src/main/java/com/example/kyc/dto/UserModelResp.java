package com.example.kyc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "User details on login response")
public record UserModelResp(
    @Schema(example = "Shariful Ahmed", description = "Full name of the user")
    @NotNull(message = "name can not be null")
    @Size(min = 2, message = "Minimum name length is 2")
    String name,

    @Schema(example = "shaarifulz@gmail.com", description = "Your email address")
    @NotNull(message = "username can not be null")
    @Size(min = 2, message = "Minimum username username is 4")
    @Email(message = "Invalid email format")
    String username,

    @Schema(example = "true|false", description = "Flag if users has selected category")
    Boolean isActive
) {}