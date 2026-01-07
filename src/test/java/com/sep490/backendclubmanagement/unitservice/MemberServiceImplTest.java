package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.response.MemberResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.SimpleMemberResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.EpuApiService;
import com.sep490.backendclubmanagement.service.member.MemberServiceImpl;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;
    @Mock
    private ClubRoleRepository clubRoleRepository;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EpuApiService epuApiService;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Club club;
    private User user;
    private ClubMemberShip cms;
    private Semester currentSemester;

    @BeforeEach
    public void setup() {
        club = new Club();
        club.setId(1L);
        club.setClubName("Test Club");

        user = new User();
        user.setId(10L);
        user.setFullName("Nguyễn Văn A");
        user.setStudentCode("S001");
        user.setEmail("a@example.com");

        cms = new ClubMemberShip();
        cms.setId(100L);
        cms.setUser(user);
        cms.setClub(club);
        cms.setJoinDate(LocalDate.of(2024,1,1));
        cms.setStatus(ClubMemberShipStatus.ACTIVE);

        currentSemester = new Semester();
        currentSemester.setId(5L);
        currentSemester.setSemesterCode("2024A");
        currentSemester.setIsCurrent(true);
        currentSemester.setStartDate(LocalDate.of(2024,1,1));
        currentSemester.setEndDate(LocalDate.of(2024,6,1));
    }

    @Test
    void getMembersWithFilters_noSearch_returnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClubMemberShip> page = new PageImpl<>(List.of(cms), pageable, 1);

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester));
        when(clubMemberShipRepository.findMembersWithFiltersOptimized(eq(club.getId()), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

        // Act
        PageResponse<MemberResponse> resp = memberService.getMembersWithFilters(club.getId(), null, null, null, null, null, pageable);

        // Assert

        assertNotNull(resp);
        assertEquals(1, resp.getTotalElements());
        assertEquals("Nguyễn Văn A", resp.getContent().get(0).getFullName());
    }

    @Test
    void getMembersWithFilters_withSearch_filtersResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        User otherUser = new User();
        otherUser.setId(11L);
        otherUser.setFullName("Trần Văn B");
        otherUser.setStudentCode("S002");
        otherUser.setEmail("b@example.com");

        ClubMemberShip otherCms = new ClubMemberShip();
        otherCms.setId(101L);
        otherCms.setUser(otherUser);
        otherCms.setClub(club);
        otherCms.setJoinDate(LocalDate.of(2024, 1, 1));
        otherCms.setStatus(ClubMemberShipStatus.ACTIVE);

        // page có 2 member, nhưng search chỉ match 1
        Page<ClubMemberShip> page = new PageImpl<>(List.of(cms, otherCms), pageable, 2);

        when(semesterRepository.findAll()).thenReturn(List.of(currentSemester));
        when(clubMemberShipRepository.findMembersWithFiltersOptimized(eq(club.getId()), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

        // Act - search theo tên "Nguyễn", kỳ vọng chỉ còn lại cms của user "Nguyễn Văn A"
        PageResponse<MemberResponse> resp = memberService.getMembersWithFilters(club.getId(), null, null, null, null, "Nguyễn", pageable);

        // Assert
        assertNotNull(resp);
        assertEquals(1, resp.getContent().size());
        assertEquals("Nguyễn Văn A", resp.getContent().get(0).getFullName());
    }

    @Test
    void updateMemberRole_createsRoleAndSendsNotification_whenAssigningToOtherUser() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L; // actor

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleName("Leader");
        clubRole.setRoleLevel(1);

        // Mock current user's membership and role
        ClubMemberShip currentUserCms = new ClubMemberShip();
        currentUserCms.setId(200L);
        currentUserCms.setUser(new User());
        currentUserCms.getUser().setId(currentUserId);
        currentUserCms.setClub(club);
        
        RoleMemberShip currentUserRm = new RoleMemberShip();
        ClubRole currentUserRole = new ClubRole();
        currentUserRole.setRoleLevel(0); // Higher level (lower number) = more permission
        currentUserRm.setClubRole(currentUserRole);
        currentUserRm.setIsActive(true);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(currentUserId))).thenReturn(currentUserCms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.of(clubRole));
        when(userService.getCurrentUserId()).thenReturn(currentUserId);
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(cms.getId()), eq(currentSemester.getId()))).thenReturn(Collections.emptyList());
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(currentUserCms.getId()), eq(currentSemester.getId()))).thenReturn(List.of(currentUserRm));
        when(clubRepository.findById(eq(clubId))).thenReturn(Optional.of(club));

        // Act
        memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId);

        // Assert: role saved and notification sent
        verify(roleMemberShipRepository, atLeastOnce()).save(any(RoleMemberShip.class));
        verify(notificationService, times(1)).sendToUser(eq(userId), eq(currentUserId), anyString(), anyString(), any(), any(), anyString(), eq(clubId), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void updateMemberActiveStatus_deactivate_removesRoleMemberships() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Semester semester = currentSemester;
        RoleMemberShip rm = new RoleMemberShip();
        rm.setId(200L);
        rm.setClubMemberShip(cms);
        rm.setSemester(semester);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(anyLong())).thenReturn(Optional.of(semester));
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(cms.getId()), eq(semester.getId()))).thenReturn(List.of(rm));

        // Act
        memberService.updateMemberActiveStatus(clubId, userId, false, semester.getId());

        // Assert
        verify(roleMemberShipRepository, times(1)).delete(eq(rm));
    }

    @Test
    void updateMemberActiveStatus_activate_createsRoleMembershipIfMissing() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Semester semester = currentSemester;
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(anyLong())).thenReturn(Optional.of(semester));
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(cms.getId()), eq(semester.getId()))).thenReturn(Collections.emptyList());

        // Act
        memberService.updateMemberActiveStatus(clubId, userId, true, semester.getId());

        // Assert
        verify(roleMemberShipRepository, times(1)).save(any(RoleMemberShip.class));
    }

    @Test
    void removeMemberFromClub_marksLeftAndSendsNotification() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(clubRepository.findById(eq(clubId))).thenReturn(Optional.of(club));

        // Act
        memberService.removeMemberFromClub(clubId, userId, "Vi phạm nội quy");

        // Assert
        assertEquals(ClubMemberShipStatus.LEFT, cms.getStatus());
        verify(clubMemberShipRepository, times(1)).save(eq(cms));
        verify(notificationService, times(1)).sendToUser(eq(userId), isNull(), contains("xóa"), anyString(), any(), any(), anyString(), eq(clubId), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void getAllActiveMembersForSelection_returnsSimpleResponses() {
        // Arrange
        when(clubMemberShipRepository.findByClubIdAndStatus(eq(club.getId()), eq(ClubMemberShipStatus.ACTIVE))).thenReturn(List.of(cms));

        // Act
        List<SimpleMemberResponse> list = memberService.getAllActiveMembersForSelection(club.getId());

        // Assert
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(user.getId(), list.get(0).getUserId());
        assertEquals(user.getFullName(), list.get(0).getFullName());
    }

    @Test
    void getLeftMembers_noSearch_returnsPage() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        cms.setStatus(ClubMemberShipStatus.LEFT);
        Page<ClubMemberShip> page = new PageImpl<>(List.of(cms), pageable, 1);

        when(clubMemberShipRepository.findLeftMembersOptimized(eq(club.getId()), eq(pageable)))
                .thenReturn(page);

        // Act
        PageResponse<MemberResponse> resp = memberService.getLeftMembers(club.getId(), null, pageable);

        // Assert
        assertNotNull(resp);
        assertEquals(1, resp.getTotalElements());
        assertEquals("Nguyễn Văn A", resp.getContent().get(0).getFullName());
    }

    @Test
    void getLeftMembers_withSearch_filtersResult() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        cms.setStatus(ClubMemberShipStatus.LEFT);

        User otherUser = new User();
        otherUser.setId(11L);
        otherUser.setFullName("Trần Văn B");
        otherUser.setStudentCode("S002");
        otherUser.setEmail("b@example.com");

        ClubMemberShip otherCms = new ClubMemberShip();
        otherCms.setId(101L);
        otherCms.setUser(otherUser);
        otherCms.setClub(club);
        otherCms.setJoinDate(LocalDate.of(2024, 1, 1));
        otherCms.setStatus(ClubMemberShipStatus.LEFT);

        Page<ClubMemberShip> page = new PageImpl<>(List.of(cms, otherCms), pageable, 2);

        when(clubMemberShipRepository.findLeftMembersOptimized(eq(club.getId()), eq(pageable)))
                .thenReturn(page);

        // Act - search theo "Nguyễn" để chỉ match member đầu tiên
        PageResponse<MemberResponse> resp = memberService.getLeftMembers(club.getId(), "Nguyễn", pageable);

        // Assert
        assertNotNull(resp);
        assertEquals(1, resp.getContent().size());
        assertEquals("Nguyễn Văn A", resp.getContent().get(0).getFullName());
    }

    @Test
    void updateMemberTeam_createsOrUpdatesRoleMembership() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long teamId = 22L;
        Long semesterId = currentSemester.getId();

        Team team = new Team();
        team.setId(teamId);
        team.setTeamName("Team A");

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(cms.getId()), eq(currentSemester.getId()))).thenReturn(Collections.emptyList());

        // Act
        memberService.updateMemberTeam(clubId, userId, teamId, semesterId);

        // Assert
        verify(roleMemberShipRepository, times(1)).save(any(RoleMemberShip.class));
        // Verify team is set
        ArgumentCaptor<RoleMemberShip> captor = ArgumentCaptor.forClass(RoleMemberShip.class);
        verify(roleMemberShipRepository).save(captor.capture());
        assertEquals(teamId, captor.getValue().getTeam().getId());
    }

    @Test
    void updateMemberRole_notificationException_doesNotBreakRoleAssignment() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleName("Leader");
        clubRole.setRoleLevel(1);

        // Mock current user's membership and role
        ClubMemberShip currentUserCms = new ClubMemberShip();
        currentUserCms.setId(200L);
        currentUserCms.setUser(new User());
        currentUserCms.getUser().setId(currentUserId);
        currentUserCms.setClub(club);
        
        RoleMemberShip currentUserRm = new RoleMemberShip();
        ClubRole currentUserRole = new ClubRole();
        currentUserRole.setRoleLevel(0); // Higher level (lower number) = more permission
        currentUserRm.setClubRole(currentUserRole);
        currentUserRm.setIsActive(true);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(currentUserId))).thenReturn(currentUserCms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.of(clubRole));
        when(userService.getCurrentUserId()).thenReturn(currentUserId);
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(cms.getId()), eq(currentSemester.getId()))).thenReturn(Collections.emptyList());
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(currentUserCms.getId()), eq(currentSemester.getId()))).thenReturn(List.of(currentUserRm));
        when(clubRepository.findById(eq(clubId))).thenReturn(Optional.of(club));

        // Mock notification to throw exception
        doThrow(new RuntimeException("Notification failed")).when(notificationService).sendToUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() -> memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));

        // Verify role was still saved
        verify(roleMemberShipRepository, atLeastOnce()).save(any(RoleMemberShip.class));
    }

    @Test
    void removeMemberFromClub_notificationException_doesNotBreakRemoval() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(clubRepository.findById(eq(clubId))).thenReturn(Optional.of(club));

        // Mock notification to throw exception
        doThrow(new RuntimeException("Notification failed")).when(notificationService).sendToUser(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        // Act & Assert: Should not throw exception
        assertDoesNotThrow(() -> memberService.removeMemberFromClub(clubId, userId, "Test reason"));

        // Verify member was still marked as LEFT
        assertEquals(ClubMemberShipStatus.LEFT, cms.getStatus());
        verify(clubMemberShipRepository, times(1)).save(eq(cms));
    }

    @Test
    void importMembersFromExcel_invalidClub_throwsException() {
        // Arrange
        Long invalidClubId = 999L;
        MultipartFile mockFile = mock(MultipartFile.class);

        when(clubRepository.findById(eq(invalidClubId))).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.importMembersFromExcel(invalidClubId, mockFile, 1L));
        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());
    }


    @Test
    void updateMemberRole_memberNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(null);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_memberLeft_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        cms.setStatus(ClubMemberShipStatus.LEFT);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_roleNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_unauthorized_nullCurrentUserId_throwsException() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleLevel(1);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.of(clubRole));
        when(userService.getCurrentUserId()).thenReturn(null);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_unauthorized_userIdMismatch_throwsException() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;
        Long differentUserId = 888L; // Different from currentUserId

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleLevel(1);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.of(clubRole));
        when(userService.getCurrentUserId()).thenReturn(differentUserId);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_insufficientPermission_throwsException() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = currentSemester.getId();
        Long currentUserId = 999L;

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleLevel(1); // Target role level

        // Mock current user's membership and role with insufficient permission
        ClubMemberShip currentUserCms = new ClubMemberShip();
        currentUserCms.setId(200L);
        currentUserCms.setUser(new User());
        currentUserCms.getUser().setId(currentUserId);
        currentUserCms.setClub(club);
        
        RoleMemberShip currentUserRm = new RoleMemberShip();
        ClubRole currentUserRole = new ClubRole();
        currentUserRole.setRoleLevel(2); // Lower level (higher number) = less permission, cannot assign level 1
        currentUserRm.setClubRole(currentUserRole);
        currentUserRm.setIsActive(true);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(currentUserId))).thenReturn(currentUserCms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.of(currentSemester));
        when(clubRoleRepository.findById(eq(roleId))).thenReturn(Optional.of(clubRole));
        when(userService.getCurrentUserId()).thenReturn(currentUserId);
        when(roleMemberShipRepository.findByClubMemberShipIdAndSemesterId(eq(currentUserCms.getId()), eq(currentSemester.getId()))).thenReturn(List.of(currentUserRm));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertEquals(ErrorCode.INSUFFICIENT_PERMISSION, exception.getErrorCode());
    }

    @Test
    void updateMemberRole_semesterNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = 999L; // Non-existent semester
        Long currentUserId = 999L;

        ClubRole clubRole = new ClubRole();
        clubRole.setId(roleId);
        clubRole.setRoleLevel(1);

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertTrue(exception.getMessage().contains("Semester not found"));
    }

    @Test
    void updateMemberRole_noCurrentSemester_throwsException() throws AppException {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long roleId = 11L;
        Long semesterId = null; // Will try to get current semester
        Long currentUserId = 999L;

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findAll()).thenReturn(Collections.emptyList()); // No current semester

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.updateMemberRole(clubId, userId, roleId, semesterId, currentUserId));
        assertTrue(exception.getMessage().contains("No current semester configured"));
    }

    // ========== Exception Tests for updateMemberTeam ==========

    @Test
    void updateMemberTeam_memberNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long teamId = 22L;
        Long semesterId = currentSemester.getId();

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(null);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberTeam(clubId, userId, teamId, semesterId));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMemberTeam_memberLeft_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long teamId = 22L;
        Long semesterId = currentSemester.getId();

        cms.setStatus(ClubMemberShipStatus.LEFT);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            memberService.updateMemberTeam(clubId, userId, teamId, semesterId));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMemberTeam_semesterNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long teamId = 22L;
        Long semesterId = 999L; // Non-existent semester

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            memberService.updateMemberTeam(clubId, userId, teamId, semesterId));
        assertTrue(exception.getMessage().contains("Semester not found"));
    }

    @Test
    void updateMemberTeam_noCurrentSemester_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long teamId = 22L;
        Long semesterId = null; // Will try to get current semester

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findAll()).thenReturn(Collections.emptyList()); // No current semester

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.updateMemberTeam(clubId, userId, teamId, semesterId));
        assertTrue(exception.getMessage().contains("No current semester configured"));
    }

    // ========== Exception Tests for updateMemberActiveStatus ==========

    @Test
    void updateMemberActiveStatus_memberNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long semesterId = currentSemester.getId();

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.updateMemberActiveStatus(clubId, userId, true, semesterId));
        assertTrue(exception.getMessage().contains("Member not found or already left club"));
    }

    @Test
    void updateMemberActiveStatus_memberLeft_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long semesterId = currentSemester.getId();

        cms.setStatus(ClubMemberShipStatus.LEFT);
        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.updateMemberActiveStatus(clubId, userId, true, semesterId));
        assertTrue(exception.getMessage().contains("Member not found or already left club"));
    }

    @Test
    void updateMemberActiveStatus_semesterNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long semesterId = 999L; // Non-existent semester

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findById(eq(semesterId))).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            memberService.updateMemberActiveStatus(clubId, userId, true, semesterId));
        assertTrue(exception.getMessage().contains("Semester not found"));
    }

    @Test
    void updateMemberActiveStatus_noCurrentSemester_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();
        Long semesterId = null; // Will try to get current semester

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(cms);
        when(semesterRepository.findAll()).thenReturn(Collections.emptyList()); // No current semester

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.updateMemberActiveStatus(clubId, userId, true, semesterId));
        assertTrue(exception.getMessage().contains("No current semester configured"));
    }

    // ========== Exception Tests for removeMemberFromClub ==========

    @Test
    void removeMemberFromClub_memberNotFound_throwsException() {
        // Arrange
        Long clubId = club.getId();
        Long userId = user.getId();

        when(clubMemberShipRepository.findByClubIdAndUserId(eq(clubId), eq(userId))).thenReturn(null);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            memberService.removeMemberFromClub(clubId, userId, "Test reason"));
        assertTrue(exception.getMessage().contains("Member not found"));
    }
}
