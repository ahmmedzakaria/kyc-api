package com.example.kyc.appconfigmodule.apiconfig;


import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@StandardApiResponses
@SecurityRequirement(name = "Bearer")
public @interface SecuredApi {
}
