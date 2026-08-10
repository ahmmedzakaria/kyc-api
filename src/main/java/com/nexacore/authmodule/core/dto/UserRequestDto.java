package com.nexacore.authmodule.core.dto;

import lombok.Data;

@Data
public class UserRequestDto {
    private Long id;
    private String username;
    /** Required on create. On update, a blank/null password leaves the existing password unchanged. */
    private String password;
    private String email;
    private String mobile;
    private String firstName;
    private String lastName;
    private boolean enabled;
}
