package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.UpdateUserProfileRequest;
import com.sep490.backendclubmanagement.dto.response.ClubMembershipProfileResponse;
import com.sep490.backendclubmanagement.dto.response.RoleInClubResponse;
import com.sep490.backendclubmanagement.dto.response.UserProfileResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.semester.SemesterService;
import com.sep490.backendclubmanagement.service.user.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho UserProfileServiceImpl (JUnit5 + Mockito)
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private SemesterService semesterService;

    @Mock
    private MultipartFile avatarFile;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    // ========= Helper =========

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@fpt.edu.vn");
        u.setFullName("User " + id);
        u.setPhoneNumber("0123456789");
        u.setStudentCode("SE" + id);
        u.setDateOfBirth(LocalDate.of(2001, 2, 19));
        // không set gender để tránh phụ thuộc enum

        u.setAvatarUrl("https://old-avatar.com/" + id);
        u.setIsActive(true);

        SystemRole role = new SystemRole();
        role.setId(1L);
        role.setRoleName("STUDENT");
        u.setSystemRole(role);
        return u;
    }

    private ClubMemberShip buildMembership(Long id) {
        Club club = new Club();
        club.setId(100L);
        club.setClubName("FPT Club");
        club.setClubCode("FPTC");
        club.setLogoUrl("https://logo.com/fpt");
        // không set status/featured nếu field là primitive/enum khó đoán

        Semester semester = new Semester();
        semester.setId(10L);
        semester.setSemesterName("Spring 2025");
        semester.setIsCurrent(true);

        Team team = new Team();
        team.setId(200L);
        team.setTeamName("Media Team");

        ClubRole clubRole = new ClubRole();
        clubRole.setId(300L);
        clubRole.setRoleName("Team Leader");
        clubRole.setRoleCode("TEAM_LEADER");
        clubRole.setRoleLevel(2);

        RoleMemberShip rms = new RoleMemberShip();
        rms.setId(400L);
        rms.setClubRole(clubRole);
        rms.setTeam(team);
        rms.setSemester(semester);
        rms.setIsActive(true);

        ClubMemberShip cms = new ClubMemberShip();
        cms.setId(id);
        cms.setClub(club);
        cms.setStatus(ClubMemberShipStatus.ACTIVE);
        // ✅ FIX: roleMemberships là Set, dùng HashSet
        cms.setRoleMemberships(new HashSet<>(List.of(rms)));
        return cms;
    }

    // =========================
    // getUserProfile
    // =========================

    @Test
    void getUserProfile_whenUserExists_shouldReturnProfile() throws AppException {
        Long userId = 1L;
        User user = buildUser(userId);
        ClubMemberShip cms = buildMembership(500L);

        Semester currentSemester = new Semester();
        currentSemester.setId(10L);
        currentSemester.setSemesterName("Spring 2025");
        currentSemester.setIsCurrent(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(semesterService.getCurrentSemester()).thenReturn(currentSemester);
        when(clubMemberShipRepository.findByUserIdWithRoles(
                eq(userId),
                eq(ClubMemberShipStatus.ACTIVE),
                isNull(),
                eq(true)
        )).thenReturn(List.of(cms));

        UserProfileResponse resp = userProfileService.getUserProfile(userId);

        assertEquals(userId, resp.getId());
        assertEquals("user1@fpt.edu.vn", resp.getEmail());
        assertEquals("User 1", resp.getFullName());
        assertEquals("SE1", resp.getStudentCode());
        assertEquals(1L, resp.getSystemRoleId());
        assertEquals("STUDENT", resp.getSystemRoleName());
        assertEquals(1, resp.getClubMemberships().size());

        ClubMembershipProfileResponse clubResp = resp.getClubMemberships().get(0);
        assertEquals(100L, clubResp.getClubId());
        assertEquals("FPT Club", clubResp.getClubName());
        assertEquals("FPTC", clubResp.getClubCode());

        assertEquals(1, clubResp.getRoles().size());
        RoleInClubResponse roleResp = clubResp.getRoles().get(0);
        assertEquals(300L, roleResp.getClubRoleId());
        assertEquals("Team Leader", roleResp.getClubRoleName());
        assertEquals("TEAM_LEADER", roleResp.getClubRoleCode());
        assertEquals(200L, roleResp.getTeamId());
        assertEquals(10L, roleResp.getSemesterId());
        assertTrue(roleResp.getSemesterIsCurrent());
    }

    @Test
    void getUserProfile_whenUserNotFound_shouldThrowAppException() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> userProfileService.getUserProfile(userId));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // =========================
    // updateUserProfile
    // =========================

    @Test
    void updateUserProfile_whenUserExists_shouldUpdateOnlyNonNullFields() throws AppException {
        Long userId = 2L;
        User user = buildUser(userId);

        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFullName("New Name");
        req.setPhoneNumber(null);               // null -> không đổi
        req.setStudentCode("SE0002");
        req.setDateOfBirth(LocalDate.of(2002, 1, 1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clubMemberShipRepository.findByUserIdWithRoles(
                eq(userId),
                eq(ClubMemberShipStatus.ACTIVE),
                isNull(),
                eq(true)
        )).thenReturn(List.of());
        when(semesterService.getCurrentSemester()).thenReturn(new Semester());

        UserProfileResponse resp = userProfileService.updateUserProfile(userId, req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("New Name", saved.getFullName());
        assertEquals("0123456789", saved.getPhoneNumber());  // giữ nguyên
        assertEquals("SE0002", saved.getStudentCode());
        assertEquals(LocalDate.of(2002, 1, 1), saved.getDateOfBirth());

        assertEquals("New Name", resp.getFullName());
        assertEquals("SE0002", resp.getStudentCode());
    }

    @Test
    void updateUserProfile_whenUserNotFound_shouldThrowAppException() {
        Long userId = 100L;
        UpdateUserProfileRequest req = new UpdateUserProfileRequest();
        req.setFullName("Any");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> userProfileService.updateUserProfile(userId, req));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // =========================
    // updateUserAvatar
    // =========================

    @Test
    void updateUserAvatar_whenValidFile_shouldUploadAndUpdateUrl() throws AppException {
        Long userId = 3L;
        User user = buildUser(userId);

        when(avatarFile.isEmpty()).thenReturn(false);

        // UploadResult có 4 tham số: url, publicId, format, bytes
        CloudinaryService.UploadResult uploadResult =
                new CloudinaryService.UploadResult(
                        "https://new-avatar.com/3",
                        "public-id-3",
                        "jpg",
                        12345L
                );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cloudinaryService.uploadImage(avatarFile, "users/avatars"))
                .thenReturn(uploadResult);

        when(clubMemberShipRepository.findByUserIdWithRoles(
                eq(userId),
                eq(ClubMemberShipStatus.ACTIVE),
                isNull(),
                eq(true)
        )).thenReturn(List.of());
        when(semesterService.getCurrentSemester()).thenReturn(new Semester());

        UserProfileResponse resp = userProfileService.updateUserAvatar(userId, avatarFile);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User saved = userCaptor.getValue();
        assertEquals("https://new-avatar.com/3", saved.getAvatarUrl());
        assertEquals("https://new-avatar.com/3", resp.getAvatarUrl());
    }

    @Test
    void updateUserAvatar_whenFileNull_shouldThrowAppException() {
        Long userId = 3L;

        AppException ex = assertThrows(AppException.class,
                () -> userProfileService.updateUserAvatar(userId, null));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        verifyNoInteractions(userRepository, cloudinaryService);
    }

    @Test
    void updateUserAvatar_whenFileEmpty_shouldThrowAppException() {
        Long userId = 3L;
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> userProfileService.updateUserAvatar(userId, emptyFile));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        verifyNoInteractions(userRepository, cloudinaryService);
    }

    @Test
    void updateUserAvatar_whenUserNotFound_shouldThrowAppException() {
        Long userId = 3L;
        when(avatarFile.isEmpty()).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> userProfileService.updateUserAvatar(userId, avatarFile));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(cloudinaryService, never()).uploadImage(any(), anyString());
    }
}
