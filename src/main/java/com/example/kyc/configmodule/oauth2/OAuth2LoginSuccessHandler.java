package com.example.kyc.configmodule.oauth2;

import com.example.kyc.configmodule.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil; // ← inject your JwtUtil

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String name = oauthUser.getAttribute("name");
        String email = oauthUser.getAttribute("email");
        String picture = oauthUser.getAttribute("picture");

        // Generate a JWT with the email as subject
        String token =  ""; //jwtUtil.generateTokenWithSubject(email, 3600000); // 1 hour token

        String redirectUri = UriComponentsBuilder
                .fromUriString("http://localhost:4200/oauth-success")
                .queryParam("token", token)
                .queryParam("name", URLEncoder.encode(name, StandardCharsets.UTF_8))
                .queryParam("email", email)
                .queryParam("picture", picture)
                .build().toUriString();

        response.sendRedirect(redirectUri);
    }
}
