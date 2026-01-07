package com.sep490.backendclubmanagement.service.auth.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.sep490.backendclubmanagement.dto.response.AuthenticationResponse;
import com.sep490.backendclubmanagement.dto.response.ClubRoleInfo;
import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.*;
import com.sep490.backendclubmanagement.service.auth.AuthService;
import com.sep490.backendclubmanagement.service.auth.GoogleTokenVerifierService;
import com.sep490.backendclubmanagement.service.auth.RefreshTokenService;
import com.sep490.backendclubmanagement.service.auth.TokenBlacklistService;
import com.sep490.backendclubmanagement.service.club.club.ClubManagementService;
import com.sep490.backendclubmanagement.service.role.SystemRoleService;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EpuApiService epuApiService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final SystemRoleService systemRoleService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final ClubManagementService clubManagementService;
    private final GoogleTokenVerifierService googleTokenVerifier;

    @Override
    public AuthenticationResponse loginWithGoogle(String idToken, HttpServletRequest request, HttpServletResponse response) throws Exception {
        GoogleIdToken.Payload payload = googleTokenVerifier.verifyIdToken(idToken);
        String email = payload.getEmail();

        Optional<Map<String, Object>> profileOpt = epuApiService.findProfileByEmail(email);
        if (profileOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.ORG_UNAUTHORIZED.getMessage());
        }

        Map<String, Object> profile = profileOpt.get();
        String fullName = Objects.toString(profile.getOrDefault("fullName", payload.get("name")), "");
        String avatarUrl = Objects.toString(profile.getOrDefault("avatarUrl", payload.get("picture")), "");
        String studentCode = Optional.ofNullable((String) profile.get("studentCode"))
                .filter(s -> !s.isBlank())
                .orElse(null);

        SystemRole role = systemRoleService.findByRoleName("STUDENT").orElseGet(SystemRole::new);

        // Handle user creation/retrieval
        User user = userService.findByEmail(email).orElse(null);

        if (user == null) {
            // Create new user with information from Google/EpuApiUrl
            user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setAvatarUrl(avatarUrl);
            user.setProvider("GOOGLE");
            user.setProviderId(payload.getSubject());
            user.setIsActive(true);
            user.setStudentCode(studentCode);
            user.setSystemRole(role);
            user = userService.save(user);
        } else {
            // Check if existing user is active
            if (!user.getIsActive()) {
                throw new RuntimeException(ErrorCode.USER_NOT_ACTIVE.getMessage());
            }
            // Ensure user has system role
            if (user.getSystemRole() == null) {
                user.setSystemRole(role);
                user = userService.save(user);
            }
        }

        // Build Spring Security user for token generation
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().getRoleName()));
        org.springframework.security.core.userdetails.User securityUser =
            new org.springframework.security.core.userdetails.User(user.getEmail(), "N/A", authorities);

        // Set authentication in SecurityContext
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            securityUser, null, authorities
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Get club roles for response only (not stored in JWT)
        List<ClubRoleInfo> clubRoleList = clubManagementService.getUserClubRoles(user.getId());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("avatarUrl", user.getAvatarUrl());

        String accessToken = jwtUtil.generateAccessToken(extraClaims, securityUser);
        String refreshToken = jwtUtil.generateRefreshToken(securityUser);

        // Store refresh token in Redis
        try {
            long refreshTokenExpiration = jwtUtil.extractExpirationTimeMillis(refreshToken);
            refreshTokenService.createRefreshToken(user, refreshToken, refreshTokenExpiration);
            log.info("Refresh token stored for user: {} with expiration: {}", user.getEmail(), refreshTokenExpiration);
        } catch (Exception e) {
            log.error("Failed to store refresh token for user: {}", user.getEmail(), e);
        }

        // Set refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, refreshToken, user.getEmail());

        AuthenticationResponse.UserInfo userInfo = AuthenticationResponse.UserInfo.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .avatarUrl(user.getAvatarUrl())
            .systemRole(user.getSystemRole().getRoleName())
            .clubRoleList(clubRoleList)
            .build();

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .user(userInfo)
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Extract refresh token from HttpOnly cookie
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            log.warn("No refresh token found in cookies");
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        // Extract email from refresh token
        String email = jwtUtil.extractUsername(refreshToken);
        if (email == null || email.trim().isEmpty()) {
            log.warn("Invalid refresh token provided");
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        // Find user in database
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("User not found for refresh: {}", email);
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.getIsActive()) {
            log.warn("Inactive user attempted refresh: {}", email);
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        // Validate refresh token against stored token in Redis
        if (!refreshTokenService.isValidRefreshToken(user.getId().toString(), refreshToken)) {
            log.warn("Invalid refresh token for user: {}", email);
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        String systemRole = user.getSystemRole() != null ? user.getSystemRole().getRoleName() : "STUDENT";

        // Build Spring Security user for token generation
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + systemRole));
        org.springframework.security.core.userdetails.User securityUser = new org.springframework.security.core.userdetails.User(
                email, "N/A", authorities
        );

        // Set authentication in SecurityContext
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            securityUser, null, authorities
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Get club roles for response only (not stored in JWT)
        List<ClubRoleInfo> clubRoleList = clubManagementService.getUserClubRoles(user.getId());

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("fullName", user.getFullName());
        extraClaims.put("avatarUrl", user.getAvatarUrl());
        extraClaims.put("systemRole", systemRole);

        String newAccessToken = jwtUtil.generateAccessToken(extraClaims, securityUser);
        String newRefreshToken = jwtUtil.generateRefreshToken(securityUser);

        // Revoke old refresh token and store new one
        refreshTokenService.revokeRefreshToken(user.getId().toString());

        try {
            long refreshTokenExpiration = jwtUtil.extractExpirationTimeMillis(newRefreshToken);
            refreshTokenService.createRefreshToken(user, newRefreshToken, refreshTokenExpiration);
            log.info("New refresh token created for user: {} with expiration: {}", email, refreshTokenExpiration);
        } catch (Exception e) {
            log.error("Failed to store new refresh token for user: {}", email, e);
        }

        // Set new refresh token as HttpOnly cookie
        setRefreshTokenCookie(response, newRefreshToken, email);

        AuthenticationResponse.UserInfo userInfo = AuthenticationResponse.UserInfo.builder()
                .id(user.getId())
                .email(email)
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .systemRole(systemRole)
                .clubRoleList(clubRoleList)
                .build();

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .user(userInfo)
                .build();
    }

    @Override
    public void logout(String accessToken, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String email = jwtUtil.extractUsername(accessToken);

        if (email == null || email.trim().isEmpty()) {
            log.warn("Invalid access token provided for logout");
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        // Find user to get user ID for refresh token revocation
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("User not found for logout: {}", email);
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        User user = userOpt.get();
        String userId = user.getId().toString();

        // Revoke access token by adding to blacklist
        try {
            String jti = jwtUtil.extractJti(accessToken);
            long exp = jwtUtil.extractExpirationTimeMillis(accessToken);
            tokenBlacklistService.revoke(jti, exp);
            log.info("Access token revoked successfully for user: {} with TTL: {}s", email, (exp - System.currentTimeMillis()) / 1000);
        } catch (Exception e) {
            log.warn("Failed to revoke access token for user: {}", email, e);
        }

        // Revoke refresh token from Redis
        try {
            refreshTokenService.revokeRefreshToken(userId);
            log.info("Refresh token revoked successfully from Redis for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to revoke refresh token from Redis for user: {}", email, e);
        }

        // Clear refresh token cookie
        clearRefreshTokenCookie(response, email);
    }

    @Override
    public boolean validateToken(String token) throws Exception {
        // Extract email from token
        String email;
        try {
            email = jwtUtil.extractUsername(token);
        } catch (Exception e) {
            log.debug("Failed to extract username from token: {}", e.getMessage());
            throw new RuntimeException("Invalid token format");
        }

        if (email == null || email.trim().isEmpty()) {
            log.debug("Email extracted from token is null or empty");
            throw new RuntimeException("Invalid token");
        }

        // Check if token is blacklisted (revoked)
        try {
            String jti = jwtUtil.extractJti(token);
            if (tokenBlacklistService.isRevoked(jti)) {
                log.debug("Token has been revoked for user: {}", email);
                throw new RuntimeException("Token has been revoked");
            }
        } catch (Exception e) {
            log.warn("Failed to check token blacklist: {}", e.getMessage());
            // Continue - if blacklist check fails, still validate other aspects
        }

        // Find user in database
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.debug("User not found for email: {}", email);
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();

        // Check if user is active
        if (!user.getIsActive()) {
            log.debug("User is inactive: {}", email);
            throw new RuntimeException("User account is inactive");
        }

        // Check if token is expired
        if (jwtUtil.isTokenExpired(token)) {
            log.debug("Token is expired for user: {}", email);
            throw new RuntimeException("Token expired");
        }

        log.debug("Token validated successfully for user: {}", email);
        return true;
    }

    @Override
    public List<ClubRoleInfo> getMyRoles(String email) throws Exception {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException(ErrorCode.UNAUTHORIZED.getMessage());
        }

        User user = userOpt.get();
        return clubManagementService.getUserClubRoles(user.getId());
    }

    // Private helper methods


    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, String email) {
        try {
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(false); // Set to false for development, true for production
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge((int) ((jwtUtil.extractExpirationTimeMillis(refreshToken) - System.currentTimeMillis()) / 1000));
            response.addCookie(refreshTokenCookie);
            log.info("Refresh token cookie set for user: {}", email);
        } catch (Exception e) {
            log.error("Failed to set refresh token cookie for user: {}", email, e);
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response, String email) {
        try {
            Cookie refreshTokenCookie = new Cookie("refreshToken", "");
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setSecure(true);
            refreshTokenCookie.setPath("/");
            refreshTokenCookie.setMaxAge(0); // Expire immediately
            response.addCookie(refreshTokenCookie);
            log.info("Refresh token cookie cleared for user: {}", email);
        } catch (Exception e) {
            log.warn("Failed to clear refresh token cookie for user: {}", email, e);
        }
    }
}


