package com.nexacore.authmodule.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.allowed-clock-skew-seconds:60}")
    private long allowedClockSkewSeconds;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.audience}")
    private String audience;

    private Key secretKey;

    @PostConstruct
    void init() {
        secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // 🔹 Generate Access Token
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .toList());
        return generateToken(claims, userDetails, JwtTokenType.ACCESS, jwtExpirationMs);
    }

    // 🔹 Generate Refresh Token
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, JwtTokenType.REFRESH, refreshExpirationMs);
    }

    private String generateToken(Map<String, Object> extraClaims,
                                 UserDetails userDetails,
                                 JwtTokenType tokenType,
                                 long expirationMs) {
        extraClaims.put("token_type", tokenType.name());

        return Jwts.builder()
                .setClaims(extraClaims)
                .setIssuer(issuer)
                .setAudience(audience)
                .setId(UUID.randomUUID().toString())
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // 🔹 Extract username
    public String extractUsername(String token) {
        String username = extractClaim(token, Claims::getSubject);
        if (username == null || username.isBlank()) {
            throw new JwtException("Token subject is missing");
        }
        return username;
    }

    // 🔹 Extract single claim
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> {
            Object roles = claims.get("roles");
            if (!(roles instanceof List<?> roleList) || roleList.stream().anyMatch(role -> !(role instanceof String))) {
                throw new JwtException("Access token roles claim is missing or malformed");
            }
            return roleList.stream().map(String.class::cast).toList();
        });
    }

    public String extractJwtId(String token) {
        String tokenId = extractClaim(token, Claims::getId);
        if (tokenId == null || tokenId.isBlank()) {
            throw new JwtException("Token ID is missing");
        }
        return tokenId;
    }

    public JwtTokenType extractTokenType(String token) {
        String value = extractClaim(token, claims -> claims.get("token_type", String.class));
        try {
            return JwtTokenType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidTokenTypeException("Token type is missing or invalid");
        }
    }

    public void requireTokenType(String token, JwtTokenType expectedType) {
        JwtTokenType actualType = extractTokenType(token);
        if (actualType != expectedType) {
            throw new InvalidTokenTypeException("Expected " + expectedType + " token but received " + actualType);
        }
    }

    public Date extractIssuedAt(String token) {
        Date issuedAt = extractClaim(token, Claims::getIssuedAt);
        if (issuedAt == null) {
            throw new JwtException("Token issued-at claim is missing");
        }
        return issuedAt;
    }

    // 🔹 Validate token
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean validateToken(String token, UserDetails userDetails, JwtTokenType expectedType) {
        requireTokenType(token, expectedType);
        return validateToken(token, userDetails);
    }

    // 🔹 Check expiration
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 🔹 Parse claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(allowedClockSkewSeconds)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
