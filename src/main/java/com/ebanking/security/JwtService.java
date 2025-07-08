package com.ebanking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service for token operations including validation and extraction.
 * 
 * Handles JWT token parsing, validation, and customer ID extraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${app.security.jwt.secret}")
    private String jwtSecret;

    @Value("${app.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extracts customer ID from JWT token.
     * 
     * @param token JWT token
     * @return Customer ID or null if not found
     */
    public String extractCustomerId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts expiration date from JWT token.
     * 
     * @param token JWT token
     * @return Expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from JWT token.
     * 
     * @param token JWT token
     * @param claimsResolver Function to resolve the claim
     * @return Claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from JWT token.
     * 
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the signing key for JWT operations.
     * 
     * @return Secret key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Checks if JWT token is expired.
     * 
     * @param token JWT token
     * @return true if expired, false otherwise
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates JWT token against user details.
     * 
     * @param token JWT token
     * @param userDetails User details
     * @return true if valid, false otherwise
     */
    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String customerId = extractCustomerId(token);
        return (customerId.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Generates JWT token for a customer.
     * 
     * @param customerId Customer ID
     * @return JWT token
     */
    public String generateToken(String customerId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, customerId);
    }

    /**
     * Creates JWT token with claims and subject.
     * 
     * @param claims Token claims
     * @param subject Token subject (customer ID)
     * @return JWT token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
} 