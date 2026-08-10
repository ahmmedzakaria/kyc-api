package com.nexacore.authmodule.security.filter;


import com.nexacore.authmodule.security.jwt.JwtUtil;
import com.nexacore.authmodule.security.jwt.JwtTokenType;
import com.nexacore.authmodule.security.jwt.InvalidTokenTypeException;
import com.nexacore.authmodule.core.service.implementations.LogoutSessionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final LogoutSessionService logoutSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // [DIAG] temporary — tracing a bug where /person/search 401s immediately
        // after a successful /person/create. Prints on every request so we can
        // see whether SecurityContextHolder already has a (stale/leaked)
        // Authentication BEFORE this filter's own logic runs, and which pooled
        // thread is handling the request (to catch cross-request ThreadLocal leakage).
        System.out.println("[DIAG] >>> " + request.getMethod() + " " + request.getRequestURI()
                + " thread=" + Thread.currentThread().getName()
                + " authHeaderPresent=" + (authHeader != null)
                + " preExistingAuth=" + SecurityContextHolder.getContext().getAuthentication());

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println(" JwtAuthenticationFilter");

            try {
                String jwt = authHeader.substring(7);
                jwtUtil.requireTokenType(jwt, JwtTokenType.ACCESS);
                String username = jwtUtil.extractUsername(jwt);
                List<String> roles = jwtUtil.extractRoles(jwt);
                Instant issuedAt = jwtUtil.extractIssuedAt(jwt).toInstant();

                boolean sessionActive = logoutSessionService.isSessionActive(username, issuedAt);
                System.out.println("[DIAG]     username=" + username + " issuedAt=" + issuedAt
                        + " sessionActive=" + sessionActive);

                if (!sessionActive) {
                    request.setAttribute("jwt_error_message",
                            "User session is not active. Please log in again.");
                    throw new JwtException("User session is not active");
                }

                boolean alreadyAuthenticated = SecurityContextHolder.getContext().getAuthentication() != null;
                System.out.println("[DIAG]     alreadyAuthenticatedBeforeSet=" + alreadyAuthenticated);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    System.out.println("[DIAG]     SET Authentication for username=" + username);
                }
            } catch (InvalidTokenTypeException ex) {
                System.out.println("[DIAG]     INVALID_TOKEN_TYPE: " + ex.getMessage());
                SecurityContextHolder.clearContext();
                request.setAttribute("jwt_error_code", "INVALID_TOKEN_TYPE");
            } catch (JwtException | IllegalArgumentException ex) {
                System.out.println("[DIAG]     AUTHENTICATION_REQUIRED (" + ex.getClass().getSimpleName()
                        + "): " + ex.getMessage());
                SecurityContextHolder.clearContext();
                request.setAttribute("jwt_error_code", "AUTHENTICATION_REQUIRED");
            }
        }

        filterChain.doFilter(request, response); // continue the chain

        // [DIAG] temporary — confirms whether the context was cleared before this
        // pooled thread is returned; a non-null Authentication surviving here is
        // exactly what would leak into the next request handled by this thread.
        System.out.println("[DIAG] <<< " + request.getMethod() + " " + request.getRequestURI()
                + " thread=" + Thread.currentThread().getName()
                + " authAfterChain=" + SecurityContextHolder.getContext().getAuthentication());
    }

//    @Override
//    protected boolean shouldNotFilter(HttpServletRequest request) {
//        String path = request.getServletPath();
//        return path.startsWith("/auth/authenticate")
//                || path.startsWith("/auth/refresh-token")
//                || path.startsWith("/swagger-ui")
//                || path.startsWith("/v3/api-docs")
//                || path.startsWith("/swagger-resources")
//                || path.startsWith("/webjars");
//    }
}
