package com.example.kyc.configmodule.security;

import com.example.kyc.configmodule.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AuthenticationProviderConfig {

    private final MyUserDetailsService userDetailsService;

    // 🔹 AuthenticationProvider
    @Bean
    public org.springframework.security.authentication.AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // 🔹 AuthenticationManager (delegates to AuthenticationProvider)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // 🔹 PasswordEncoder (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return NoOpPasswordEncoder.getInstance(); // Plain Text Password
//    }
/*
    🔹 How It Works in Flow

    AuthController receives username/password → calls AuthenticationManager.authenticate().

    AuthenticationManager delegates to DaoAuthenticationProvider.

    DaoAuthenticationProvider loads user with CustomUserDetailsService and checks password with PasswordEncoder.

    If valid → returns Authentication with roles, which you use to generate JWT.
 */


}

