package com.example.kyc.security;


import com.example.kyc.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    private static final List<String> EXCLUDE_URLS = List.of(
            "/auth/authenticate",
           // "/api/kyc/create",
            "/auth/refresh",
            "/swagger-ui",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/v3/api-docs"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token=null;
        String username=null;

        try {
            String requestURI = request.getRequestURI();

            // ✅ Skip excluded URLs
            if (EXCLUDE_URLS.stream().anyMatch(requestURI::startsWith)) {
                filterChain.doFilter(request, response);
                return;
            }

        	if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                username = jwtUtil.extractUsername(token);
            }
        	if (jwtUtil.isTokenExpired(token)) {
				request.setAttribute("exception", "Token has expired, try to get new access token to continue!"); //expired
        	}
            if (jwtUtil.isTokenValid(token)) {
        		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.isTokenValid(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
		} catch(ExpiredJwtException ex){
			request.setAttribute("exception", ex); //expired
		} catch(BadCredentialsException ex){
			request.setAttribute("exception", ex); //does not matched
		} catch(MalformedJwtException ex){
			request.setAttribute("exception", ex); //does not matched
		} catch(IllegalArgumentException ex){
			request.setAttribute("exception", ex); //does not matched
		} catch(Exception ex){
			request.setAttribute("exception", ex); //does not matched
		}
        
        filterChain.doFilter(request, response);
    }


}
