package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.GoogleLoginRequest;
import com.sep490.backendclubmanagement.dto.response.AuthenticationResponse;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            AuthenticationResponse auth = authService.loginWithGoogle(
                request.getIdToken(),
                httpRequest,
                httpResponse
            );
            return ApiResponse.success(auth);
        } catch (Exception e) {
            log.error("Error during Google login: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * Refresh token
     */
    @PostMapping("/refreshToken")
    public ApiResponse<AuthenticationResponse> refreshToken(
            HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthenticationResponse auth = authService.refreshToken(request, response);
            return ApiResponse.success(auth);
        } catch (Exception e) {
            log.error("Error refreshing token server-side: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.UNAUTHORIZED, null);
        }
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            HttpServletRequest request, HttpServletResponse response) {
        
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("No valid authorization header provided for logout");
                return ApiResponse.error(ErrorCode.UNAUTHORIZED, null);
            }
            
            String accessToken = authorization.substring(7);
            authService.logout(accessToken, request, response);
            return ApiResponse.success("Logout successful");
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, null);
        }
    }

    /**
     * API endpoint để validate JWT token
     * Kiểm tra token có hợp lệ, chưa hết hạn, chưa bị revoke, và user còn active
     *
     * Standard validation endpoint - returns true/false only
     *
     * @param authorization Authorization header với Bearer token
     * @return true nếu token valid, 401 error nếu invalid
     */
    @GetMapping("/validate")
    public ApiResponse<Boolean> validateToken(
            @RequestHeader(name = "Authorization", required = false) String authorization) {
        try {
            // Check authorization header
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.debug("No valid Authorization header for validation");
                return ApiResponse.error(ErrorCode.UNAUTHORIZED, "Missing or invalid Authorization header", null);
            }

            String token = authorization.substring(7);

            try {
                boolean isValid = authService.validateToken(token);
                return ApiResponse.success(isValid);
            } catch (Exception e) {
                log.debug("Token validation failed: {}", e.getMessage());
                return ApiResponse.error(ErrorCode.UNAUTHORIZED, e.getMessage(), null);
            }
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, "Token validation error", null);
        }
    }

    /**
     * API endpoint để refresh club roles của user hiện tại
     * Dùng khi cần cập nhật role data trong localStorage
     */
    @GetMapping("/my-roles")
    public ApiResponse<List<com.sep490.backendclubmanagement.dto.response.ClubRoleInfo>> getMyRoles() {
        try {
            // Lấy user hiện tại từ SecurityContext
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ApiResponse.error(ErrorCode.UNAUTHORIZED, null);
            }

            String email = auth.getName();
            List<com.sep490.backendclubmanagement.dto.response.ClubRoleInfo> clubRoleList =
                    authService.getMyRoles(email);

            return ApiResponse.success(clubRoleList);
        } catch (Exception e) {
            log.error("Error getting user roles: {}", e.getMessage(), e);
            return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR, null);
        }
    }
}


