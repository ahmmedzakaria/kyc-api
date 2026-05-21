package com.nexacore.appconfigmodule.security;

import com.nexacore.appconfigmodule.ConfigConstants;
import com.nexacore.appconfigmodule.filters.JwtAuthenticationFilter;
import com.nexacore.appconfigmodule.jwt.JwtAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthenticationProviderConfig authenticationProviderConfig;
    private final JwtAuthEntryPoint authenticationEntryPoint;

    private final String[] AUTH_WHITELIST = {
            "/auth/login",
            "/auth/authenticate",
            "/auth/config",
            "/auth/sso/authenticate",
            "/auth/login-status",
            "/oauth2/**",
            "/users/register",
            "/auth/test",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "http://localhost:4200",
            "http://localhost:4300"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_WHITELIST).permitAll()
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // expects ROLE_ADMIN
//                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProviderConfig.authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:4300"));
                    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
                    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Silent"));
                    return config;
                }));

        return http.build();
    }

}
