package com.nexacore.authmodule.security.config;

import com.nexacore.appconfigmodule.ConfigConstants;
import com.nexacore.authmodule.security.filter.JwtAuthenticationFilter;
import com.nexacore.authmodule.security.jwt.JwtAuthEntryPoint;
import com.nexacore.systemmodule.accesscontrol.security.ClientApiAccessFilter;
import com.nexacore.systemmodule.accesscontrol.security.AuthenticatedRequestContextFilter;
import com.nexacore.systemmodule.accesscontrol.security.ClientApplicationAuthenticationFilter;
import com.nexacore.systemmodule.accesscontrol.security.UserPrivilegeApiAccessFilter;
import com.nexacore.systemmodule.accesscontrol.security.PublicRoutePolicy;
import com.nexacore.systemmodule.accesscontrol.security.AccessControlError;
import com.nexacore.systemmodule.accesscontrol.security.DataScopeAccessDeniedException;
import com.nexacore.commonmodule.web.ApiResponseJsonWriter;
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

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ClientApplicationAuthenticationFilter clientApplicationAuthenticationFilter;
    private final ClientApiAccessFilter clientApiAccessFilter;
    private final UserPrivilegeApiAccessFilter userPrivilegeApiAccessFilter;
    private final AuthenticatedRequestContextFilter authenticatedRequestContextFilter;
    private final AuthenticationProviderConfig authenticationProviderConfig;
    private final JwtAuthEntryPoint authenticationEntryPoint;
    private final PublicRoutePolicy publicRoutePolicy;
    private final CorsProperties corsProperties;
    private final ApiResponseJsonWriter responseWriter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler((request, response, exception) -> {
                            AccessControlError error = exception instanceof DataScopeAccessDeniedException
                                    ? AccessControlError.DATA_SCOPE_NOT_ALLOWED
                                    : AccessControlError.USER_PRIVILEGE_NOT_ALLOWED;
                            responseWriter.writeError(response, error.getStatus(), error.name(), error.getMessage());
                        }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicRoutePolicy.patterns()).permitAll()
//                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // expects ROLE_ADMIN
//                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProviderConfig.authenticationProvider())
                .addFilterBefore(clientApplicationAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(clientApiAccessFilter, ClientApplicationAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authenticatedRequestContextFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(userPrivilegeApiAccessFilter, AuthenticatedRequestContextFilter.class)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(corsProperties.getAllowedOrigins());
                    config.setAllowedMethods(corsProperties.getAllowedMethods());
                    config.setAllowedHeaders(corsProperties.getAllowedHeaders());
                    config.setExposedHeaders(corsProperties.getExposedHeaders());
                    config.setAllowCredentials(corsProperties.isAllowCredentials());
                    config.setMaxAge(corsProperties.getMaxAgeSeconds());
                    return config;
                }));

        return http.build();
    }

}
