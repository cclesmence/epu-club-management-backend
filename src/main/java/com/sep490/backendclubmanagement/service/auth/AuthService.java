package com.sep490.backendclubmanagement.service.auth;

import com.sep490.backendclubmanagement.dto.response.AuthenticationResponse;
import com.sep490.backendclubmanagement.dto.response.ClubRoleInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface AuthService {

    /**
     * Login with Google ID token
     */
    AuthenticationResponse loginWithGoogle(String idToken, HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Refresh access token using refresh token from cookie
     */
    AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Logout user by revoking tokens
     */
    void logout(String accessToken, HttpServletRequest request, HttpServletResponse response) throws Exception;

    /**
     * Validate access token
     */
    boolean validateToken(String token) throws Exception;

    /**
     * Get club roles for current authenticated user
     */
    List<ClubRoleInfo> getMyRoles(String email) throws Exception;
}
