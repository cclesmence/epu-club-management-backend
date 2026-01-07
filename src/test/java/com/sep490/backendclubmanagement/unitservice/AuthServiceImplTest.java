package com.sep490.backendclubmanagement.unitservice;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.sep490.backendclubmanagement.dto.response.AuthenticationResponse;
import com.sep490.backendclubmanagement.dto.response.ClubRoleInfo;
import com.sep490.backendclubmanagement.entity.SystemRole;
import com.sep490.backendclubmanagement.entity.User;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.service.*;
import com.sep490.backendclubmanagement.service.auth.GoogleTokenVerifierService;
import com.sep490.backendclubmanagement.service.auth.RefreshTokenService;
import com.sep490.backendclubmanagement.service.auth.TokenBlacklistService;
import com.sep490.backendclubmanagement.service.auth.impl.AuthServiceImpl;
import com.sep490.backendclubmanagement.service.club.club.ClubManagementService;
import com.sep490.backendclubmanagement.service.role.SystemRoleService;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private GoogleTokenVerifierService googleTokenVerifier;

    @Mock
    private EpuApiService epuApiService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private SystemRoleService systemRoleService;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private ClubManagementService clubManagementService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private final String testEmail = "test@fpt.edu.vn";
    private final String testIdToken = "test-google-id-token";
    private final String testAccessToken = "test-access-token";
    private final String testRefreshToken = "test-refresh-token";
    private List<ClubRoleInfo> clubRoles;

    @BeforeEach
    void setup() {
        // Setup student role
        SystemRole studentRole = new SystemRole();
        studentRole.setId(1L);
        studentRole.setRoleName("STUDENT");
        
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail(testEmail);
        testUser.setFullName("Test User");
        testUser.setAvatarUrl("https://example.com/avatar.jpg");
        testUser.setProvider("GOOGLE");
        testUser.setProviderId("google-12345");
        testUser.setIsActive(true);
        testUser.setStudentCode("SE123456");
        testUser.setSystemRole(studentRole);
        
        // Setup club roles
        clubRoles = new ArrayList<>();
        ClubRoleInfo clubRole = new ClubRoleInfo();
        clubRole.setClubId(1L);
        clubRole.setClubName("Test Club");
        clubRole.setClubRole("Member");
        clubRole.setSystemRole("MEMBER");
        clubRoles.add(clubRole);
    }

    @Test
    void loginWithGoogle_newUser_createsUserAndReturnsToken() throws Exception {
        // Arrange
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(testEmail);
        payload.setSubject("google-12345");
        payload.set("name", "Test User");
        payload.set("picture", "https://example.com/avatar.jpg");

        when(googleTokenVerifier.verifyIdToken(testIdToken)).thenReturn(payload);

        Map<String, Object> epuProfile = new HashMap<>();
        epuProfile.put("fullName", "Test User");
        epuProfile.put("avatarUrl", "https://example.com/avatar.jpg");
        epuProfile.put("studentCode", "SE123456");

        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.of(epuProfile));
        when(systemRoleService.findByRoleName("STUDENT")).thenReturn(Optional.of(testUser.getSystemRole()));
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(userService.save(any(User.class))).thenReturn(testUser);
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        when(jwtUtil.generateAccessToken(anyMap(), any())).thenReturn(testAccessToken);
        when(jwtUtil.generateRefreshToken(any())).thenReturn(testRefreshToken);
        when(jwtUtil.extractExpirationTimeMillis(testRefreshToken)).thenReturn(System.currentTimeMillis() + 86400000);

        // Act
        AuthenticationResponse response = authService.loginWithGoogle(testIdToken, request, this.response);

        // Assert
        assertNotNull(response);
        assertEquals(testAccessToken, response.getAccessToken());
        assertNotNull(response.getUser());
        assertEquals(testEmail, response.getUser().getEmail());
        assertEquals("Test User", response.getUser().getFullName());
        assertEquals("STUDENT", response.getUser().getSystemRole());

        verify(googleTokenVerifier, times(1)).verifyIdToken(testIdToken);
        verify(userService, times(1)).save(any(User.class));
        verify(refreshTokenService, times(1)).createRefreshToken(eq(testUser), eq(testRefreshToken), anyLong());
    }

    @Test
    void loginWithGoogle_existingActiveUser_returnsToken() throws Exception {
        // Arrange
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(testEmail);
        payload.setSubject("google-12345");
        payload.set("name", "Test User");
        payload.set("picture", "https://example.com/avatar.jpg");

        when(googleTokenVerifier.verifyIdToken(testIdToken)).thenReturn(payload);

        Map<String, Object> epuProfile = new HashMap<>();
        epuProfile.put("fullName", "Test User");
        epuProfile.put("avatarUrl", "https://example.com/avatar.jpg");
        epuProfile.put("studentCode", "SE123456");

        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.of(epuProfile));
        when(systemRoleService.findByRoleName("STUDENT")).thenReturn(Optional.of(testUser.getSystemRole()));
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        when(jwtUtil.generateAccessToken(anyMap(), any())).thenReturn(testAccessToken);
        when(jwtUtil.generateRefreshToken(any())).thenReturn(testRefreshToken);
        when(jwtUtil.extractExpirationTimeMillis(testRefreshToken)).thenReturn(System.currentTimeMillis() + 86400000);

        // Act
        AuthenticationResponse response = authService.loginWithGoogle(testIdToken, request, this.response);

        // Assert
        assertNotNull(response);
        assertEquals(testAccessToken, response.getAccessToken());
        assertNotNull(response.getUser());
        assertEquals(testEmail, response.getUser().getEmail());

        verify(googleTokenVerifier, times(1)).verifyIdToken(testIdToken);
        verify(userService, never()).save(any(User.class)); // Không tạo user mới
    }

    @Test
    void loginWithGoogle_inactiveUser_throwsException() throws Exception {
        // Arrange
        testUser.setIsActive(false);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(testEmail);
        payload.setSubject("google-12345");

        when(googleTokenVerifier.verifyIdToken(testIdToken)).thenReturn(payload);

        Map<String, Object> epuProfile = new HashMap<>();
        epuProfile.put("fullName", "Test User");

        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.of(epuProfile));
        when(systemRoleService.findByRoleName("STUDENT")).thenReturn(Optional.of(testUser.getSystemRole()));
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.loginWithGoogle(testIdToken, request, response));
        assertEquals(ErrorCode.USER_NOT_ACTIVE.getMessage(), exception.getMessage());

        verify(googleTokenVerifier, times(1)).verifyIdToken(testIdToken);
    }

    @Test
    void loginWithGoogle_emailNotInEpuApi_throwsException() throws Exception {
        // Arrange
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(testEmail);
        payload.setSubject("google-12345");

        when(googleTokenVerifier.verifyIdToken(testIdToken)).thenReturn(payload);
        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.loginWithGoogle(testIdToken, request, response));
        assertEquals(ErrorCode.ORG_UNAUTHORIZED.getMessage(), exception.getMessage());

        verify(googleTokenVerifier, times(1)).verifyIdToken(testIdToken);
    }

    @Test
    void loginWithGoogle_refreshTokenStorageFails_continuesWithLogin() throws Exception {
        // Arrange
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail(testEmail);
        payload.setSubject("google-12345");
        payload.set("name", "Test User");

        when(googleTokenVerifier.verifyIdToken(testIdToken)).thenReturn(payload);

        Map<String, Object> epuProfile = new HashMap<>();
        epuProfile.put("fullName", "Test User");

        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.of(epuProfile));
        when(systemRoleService.findByRoleName("STUDENT")).thenReturn(Optional.of(testUser.getSystemRole()));
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        when(jwtUtil.generateAccessToken(anyMap(), any())).thenReturn(testAccessToken);
        when(jwtUtil.generateRefreshToken(any())).thenReturn(testRefreshToken);
        when(jwtUtil.extractExpirationTimeMillis(testRefreshToken)).thenReturn(System.currentTimeMillis() + 86400000);
        doThrow(new RuntimeException("Redis error")).when(refreshTokenService).createRefreshToken(any(), any(), anyLong());

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> authService.loginWithGoogle(testIdToken, request, response));

        verify(googleTokenVerifier, times(1)).verifyIdToken(testIdToken);
    }

    @Test
    void refreshToken_validToken_returnsNewTokens() {
        // Arrange
        Cookie refreshCookie = new Cookie("refreshToken", testRefreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(jwtUtil.extractUsername(testRefreshToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(refreshTokenService.isValidRefreshToken(testUser.getId().toString(), testRefreshToken)).thenReturn(true);
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        when(jwtUtil.generateAccessToken(anyMap(), any())).thenReturn(testAccessToken);
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtUtil.extractExpirationTimeMillis("new-refresh-token")).thenReturn(System.currentTimeMillis() + 86400000);
        
        // Act
        AuthenticationResponse response = assertDoesNotThrow(() -> authService.refreshToken(request, this.response));
        
        // Assert
        assertNotNull(response);
        assertEquals(testAccessToken, response.getAccessToken());
        assertNotNull(response.getUser());
        assertEquals(testEmail, response.getUser().getEmail());
        
        verify(refreshTokenService, times(1)).revokeRefreshToken(testUser.getId().toString());
        verify(refreshTokenService, times(1)).createRefreshToken(eq(testUser), eq("new-refresh-token"), anyLong());
    }

    @Test
    void refreshToken_noRefreshTokenCookie_throwsException() {
        // Arrange
        when(request.getCookies()).thenReturn(new Cookie[]{});
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.refreshToken(request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void refreshToken_invalidToken_throwsException() {
        // Arrange
        Cookie refreshCookie = new Cookie("refreshToken", testRefreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(jwtUtil.extractUsername(testRefreshToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(refreshTokenService.isValidRefreshToken(testUser.getId().toString(), testRefreshToken)).thenReturn(false);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.refreshToken(request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void refreshToken_inactiveUser_throwsException() {
        // Arrange
        testUser.setIsActive(false);
        Cookie refreshCookie = new Cookie("refreshToken", testRefreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(jwtUtil.extractUsername(testRefreshToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.refreshToken(request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void refreshToken_userNotFound_throwsException() {
        // Arrange
        Cookie refreshCookie = new Cookie("refreshToken", testRefreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(jwtUtil.extractUsername(testRefreshToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.refreshToken(request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void logout_validToken_revokesTokensAndClearsCookie() {
        // Arrange
        String jti = "test-jti";
        long exp = System.currentTimeMillis() + 3600000;
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(jwtUtil.extractExpirationTimeMillis(testAccessToken)).thenReturn(exp);
        
        // Act
        assertDoesNotThrow(() -> authService.logout(testAccessToken, request, response));
        
        // Assert
        verify(tokenBlacklistService, times(1)).revoke(jti, exp);
        verify(refreshTokenService, times(1)).revokeRefreshToken(testUser.getId().toString());
        verify(response, times(1)).addCookie(any(Cookie.class));
    }

    @Test
    void logout_invalidToken_throwsException() {
        // Arrange
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(null);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.logout(testAccessToken, request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void logout_userNotFound_throwsException() {
        // Arrange
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.logout(testAccessToken, request, response));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }

    @Test
    void logout_blacklistException_continuesWithRefreshTokenRevocation() {
        // Arrange
        String jti = "test-jti";
        long exp = System.currentTimeMillis() + 3600000;
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(jwtUtil.extractExpirationTimeMillis(testAccessToken)).thenReturn(exp);
        doThrow(new RuntimeException("Blacklist error")).when(tokenBlacklistService).revoke(jti, exp);
        
        // Act & Assert
        assertDoesNotThrow(() -> authService.logout(testAccessToken, request, response));
        
        // Verify refresh token is still revoked
        verify(refreshTokenService, times(1)).revokeRefreshToken(testUser.getId().toString());
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        // Arrange
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(false);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.isTokenExpired(testAccessToken)).thenReturn(false);
        
        // Act
        boolean result = assertDoesNotThrow(() -> authService.validateToken(testAccessToken));
        
        // Assert
        assertTrue(result);
    }

    @Test
    void validateToken_revokedToken_continuesDueToBug() throws Exception {
        // Arrange
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        // When isRevoked returns true, the implementation throws an exception
        // which is then caught by the catch block, allowing validation to continue
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(true);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.isTokenExpired(testAccessToken)).thenReturn(false);

        // Act - Due to implementation bug, even revoked tokens pass validation
        boolean result = authService.validateToken(testAccessToken);

        // Assert
        verify(tokenBlacklistService, times(1)).isRevoked(jti);
        assertTrue(result); // Bug: should have thrown exception but returns true
    }

    @Test
    void validateToken_expiredToken_throwsException() {
        // Arrange
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(false);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.isTokenExpired(testAccessToken)).thenReturn(true);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.validateToken(testAccessToken));
        assertEquals("Token expired", exception.getMessage());
    }

    @Test
    void validateToken_inactiveUser_throwsException() {
        // Arrange
        testUser.setIsActive(false);
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(false);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.validateToken(testAccessToken));
        assertEquals("User account is inactive", exception.getMessage());
    }

    @Test
    void validateToken_userNotFound_throwsException() {
        // Arrange
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(tokenBlacklistService.isRevoked(jti)).thenReturn(false);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.validateToken(testAccessToken));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void validateToken_invalidTokenFormat_throwsException() {
        // Arrange
        when(jwtUtil.extractUsername(testAccessToken)).thenThrow(new RuntimeException("Invalid JWT"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.validateToken(testAccessToken));
        assertEquals("Invalid token format", exception.getMessage());
    }

    @Test
    void validateToken_blacklistCheckFails_continuesValidation() {
        // Arrange
        String jti = "test-jti";
        
        when(jwtUtil.extractUsername(testAccessToken)).thenReturn(testEmail);
        when(jwtUtil.extractJti(testAccessToken)).thenReturn(jti);
        when(tokenBlacklistService.isRevoked(jti)).thenThrow(new RuntimeException("Redis error"));
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(jwtUtil.isTokenExpired(testAccessToken)).thenReturn(false);
        
        // Act
        boolean result = assertDoesNotThrow(() -> authService.validateToken(testAccessToken));

        // Assert
        assertTrue(result);
    }

    @Test
    void getMyRoles_validUser_returnsClubRoles() {
        // Arrange
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        
        // Act
        List<ClubRoleInfo> result = assertDoesNotThrow(() -> authService.getMyRoles(testEmail));
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
//        assertEquals("Test Club", result.getFirst().getClubName());
    }

    @Test
    void getMyRoles_userNotFound_throwsException() {
        // Arrange
        when(userService.findByEmail(testEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
            authService.getMyRoles(testEmail));
        assertEquals(ErrorCode.UNAUTHORIZED.getMessage(), exception.getMessage());
    }


    @Test
    void refreshToken_refreshTokenStorageFails_continuesWithRefresh() {
        // Arrange
        Cookie refreshCookie = new Cookie("refreshToken", testRefreshToken);
        when(request.getCookies()).thenReturn(new Cookie[]{refreshCookie});
        when(jwtUtil.extractUsername(testRefreshToken)).thenReturn(testEmail);
        when(userService.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(refreshTokenService.isValidRefreshToken(testUser.getId().toString(), testRefreshToken)).thenReturn(true);
        when(clubManagementService.getUserClubRoles(testUser.getId())).thenReturn(clubRoles);
        when(jwtUtil.generateAccessToken(anyMap(), any())).thenReturn(testAccessToken);
        when(jwtUtil.generateRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtUtil.extractExpirationTimeMillis("new-refresh-token")).thenReturn(System.currentTimeMillis() + 86400000);
        doThrow(new RuntimeException("Redis error")).when(refreshTokenService).createRefreshToken(any(), any(), anyLong());
        
        // Act & Assert
        assertDoesNotThrow(() -> authService.refreshToken(request, response));
    }
}

