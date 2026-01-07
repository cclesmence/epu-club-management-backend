package com.sep490.backendclubmanagement.util;


import com.sep490.backendclubmanagement.service.auth.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {


    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;

    /**
     * Generate access token with user details and authorities
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return generateToken(claims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate access token with extra claims
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return generateToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate refresh token
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String jti = UUID.randomUUID().toString();
        claims.put("jti", jti);
        return generateToken(claims, userDetails, refreshTokenExpiration);
    }

    /**
     * Generate token with custom expiration
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .id(jti)
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract JWT ID (JTI) from token
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract expiration time in milliseconds from token
     */
    public long extractExpirationTimeMillis(String token) {
        return extractExpiration(token).getTime();
    }

    /**
     * Extract authorities from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("authorities", List.class);
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token with error handling
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.error("JWT token is invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Check if token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token (check expiration, username, and invalidation)
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            final String username = extractUsername(token);
            final String jti = extractJti(token);
            boolean revoked = tokenBlacklistService != null && tokenBlacklistService.isRevoked(jti);
            return (username.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && !revoked);
        } catch (JwtException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }



    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

