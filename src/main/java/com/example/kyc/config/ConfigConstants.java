package com.example.kyc.config;

public class ConfigConstants {
    public static final String[] EXCLUDE_AUTH_URLS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/auth/authenticate",
            "/oauth2/**",
            "/users/register",
            "/auth/test",
    };

    public static final String[] CROS_ORIGIN_URLS = {
            "http://localhost:4200"
    };

    public static final String[] ALLOWED_REQUEST_METHODS = {"GET", "POST", "OPTIONS"};
}
