package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.CreateClubRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubInfoRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRequest;
import com.sep490.backendclubmanagement.dto.response.ClubDetailData;
import com.sep490.backendclubmanagement.dto.response.ClubManagementResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.*;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.ClubMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.*;
import com.sep490.backendclubmanagement.service.club.club.ClubService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.role.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMapper clubMapper;

    @Mock
    private CampusRepository campusRepository;

    @Mock
    private ClubCategoryRepository clubCategoryRepository;

    @Mock
    private EpuApiService epuApiService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubRoleRepository clubRoleRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private ClubService clubService;

    private Club testClub;
    private Campus testCampus;
    private ClubCategory testCategory;
    private User testUser;
    private Semester testSemester;
    private ClubDetailData testClubDetailData;
    private ClubManagementResponse testClubManagementResponse;

    private final Long testClubId = 1L;
    private final Long testUserId = 1L;
    private final Long testStaffId = 2L;
    private final Long testCampusId = 1L;
    private final Long testCategoryId = 1L;
    private final String testClubCode = "CLUB001";
    private final String testClubName = "Test Club";
    private final String testEmail = "test@fpt.edu.vn";

    @BeforeEach
    void setup() {
        // Setup campus
        testCampus = new Campus();
        testCampus.setId(testCampusId);
        testCampus.setCampusName("FPT Hà Nội");
        testCampus.setCampusCode("HN");

        // Setup category
        testCategory = ClubCategory.builder()
                .id(testCategoryId)
                .categoryName("Thể thao")
                .build();

        // Setup user
        testUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .fullName("Test User")
                .studentCode("SE123456")
                .isActive(true)
                .build();

        // Setup semester
        testSemester = new Semester();
        testSemester.setId(1L);
        testSemester.setIsCurrent(true);
        testSemester.setSemesterCode("FALL2024");

        // Setup club
        testClub = Club.builder()
                .id(testClubId)
                .clubCode(testClubCode)
                .clubName(testClubName)
                .description("Test Description")
                .status("ACTIVE")
                .campus(testCampus)
                .clubCategory(testCategory)
                .clubMemberships(new HashSet<>())
                .build();

        // Setup DTOs
        testClubDetailData = new ClubDetailData();
        testClubDetailData.setId(testClubId);
        testClubDetailData.setClubCode(testClubCode);
        testClubDetailData.setClubName(testClubName);

        testClubManagementResponse = new ClubManagementResponse();
        testClubManagementResponse.setId(testClubId);
        testClubManagementResponse.setClubCode(testClubCode);
        testClubManagementResponse.setClubName(testClubName);
    }

    // ================= getClubDetail Tests =================

    @Test
    void getClubDetail_existingClub_returnsClubDetailData() throws AppException {
        // Arrange
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(true);

        // Act
        ClubDetailData result = clubService.getClubDetail(testClubId);

        // Assert
        assertNotNull(result);
        assertEquals(testClubId, result.getId());
        assertEquals(10L, result.getTotalMembers());
        assertEquals(5L, result.getTotalEvents());
        assertEquals(3L, result.getTotalNews());
        assertTrue(result.getIsRecruiting());

        verify(clubRepository, times(1)).findByIdWithDetails(testClubId);
        verify(clubMapper, times(1)).toClubDetailData(testClub);
    }

    @Test
    void getClubDetail_nonExistingClub_throwsAppException() {
        // Arrange
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.getClubDetail(testClubId));
        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());

        verify(clubRepository, times(1)).findByIdWithDetails(testClubId);
        verify(clubMapper, never()).toClubDetailData(any());
    }

    // ================= getClubDetailByCode Tests =================

    @Test
    void getClubDetailByCode_existingClub_returnsClubDetailData() throws AppException {
        // Arrange
        when(clubRepository.findByClubCodeWithDetails(testClubCode)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.getClubDetailByCode(testClubCode);

        // Assert
        assertNotNull(result);
        assertEquals(testClubId, result.getId());
        assertEquals(testClubCode, result.getClubCode());

        verify(clubRepository, times(1)).findByClubCodeWithDetails(testClubCode);
    }

    @Test
    void getClubDetailByCode_nonExistingClub_throwsAppException() {
        // Arrange
        when(clubRepository.findByClubCodeWithDetails(testClubCode)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.getClubDetailByCode(testClubCode));
        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());

        verify(clubRepository, times(1)).findByClubCodeWithDetails(testClubCode);
    }

    // ================= getClubsByFilter Tests =================

    @Test
    void getClubsByFilter_noKeyword_returnsPageResponseWith1BasedPagination() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10); // Request page 0
        Page<Club> clubPage = new PageImpl<>(List.of(testClub), pageable, 1);

        when(clubRepository.getAllClubsByFilter(null, null, null, null, pageable))
                .thenReturn(clubPage);
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);

        // Act
        PageResponse<ClubManagementResponse> result = clubService.getClubsByFilter(
                null, null, null, null, pageable, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(10, result.getContent().get(0).getTotalMembers());

        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber(), "Page number should be 1-based (page 0 becomes page 1)");
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());

        verify(clubRepository, times(1)).countMembersByClubId(testClubId);
        verify(clubRepository, times(1)).getAllClubsByFilter(null, null, null, null, pageable);
    }

    @Test
    void getClubsByFilter_page1_returnsPageNumber2() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(1, 10); // Request page 1 (second page)
        Page<Club> clubPage = new PageImpl<>(List.of(testClub), pageable, 15);

        when(clubRepository.getAllClubsByFilter(null, null, null, null, pageable))
                .thenReturn(clubPage);
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(5L);

        // Act
        PageResponse<ClubManagementResponse> result = clubService.getClubsByFilter(
                null, null, null, null, pageable, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getPageNumber(), "Page 1 (0-indexed) should become page 2 (1-based)");
        assertEquals(5, result.getContent().get(0).getTotalMembers());
        assertTrue(result.isHasPrevious(), "Page 2 should have previous");
        assertFalse(result.isHasNext(), "Last page should not have next");

        verify(clubRepository, times(1)).countMembersByClubId(testClubId);
    }

    @Test
    void getClubsByFilter_multipleClubs_countsEachClubSeparately() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Club club2 = Club.builder().id(2L).clubCode("CLUB002").clubName("Club 2").build();
        Club club3 = Club.builder().id(3L).clubCode("CLUB003").clubName("Club 3").build();

        Page<Club> clubPage = new PageImpl<>(List.of(testClub, club2, club3), pageable, 3);

        when(clubRepository.getAllClubsByFilter(null, null, null, null, pageable))
                .thenReturn(clubPage);
        when(clubMapper.toClubManagementResponse(any(Club.class))).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(1L)).thenReturn(10L);
        when(clubRepository.countMembersByClubId(2L)).thenReturn(5L);
        when(clubRepository.countMembersByClubId(3L)).thenReturn(8L);

        // Act
        PageResponse<ClubManagementResponse> result = clubService.getClubsByFilter(
                null, null, null, null, pageable, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertEquals(1, result.getPageNumber(), "Should return 1-based page number");

        // Verify countMembersByClubId is called for each club
        verify(clubRepository, times(1)).countMembersByClubId(1L);
        verify(clubRepository, times(1)).countMembersByClubId(2L);
        verify(clubRepository, times(1)).countMembersByClubId(3L);
    }

    @Test
    void getClubsByFilter_withKeyword_returnsFilteredPage() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Club> allClubs = new PageImpl<>(List.of(testClub), PageRequest.of(0, Integer.MAX_VALUE), 1);

        when(clubRepository.getAllClubsByFilter(isNull(), isNull(), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(allClubs);
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(5L);

        // Act
        PageResponse<ClubManagementResponse> result = clubService.getClubsByFilter(
                "test", null, null, null, pageable, testStaffId);

        // Assert
        assertNotNull(result);
        assertTrue(result.getContent().size() <= 1);
        assertEquals(1, result.getPageNumber(), "Should return 1-based page number");

        verify(clubRepository, times(1)).countMembersByClubId(testClubId);
    }

    @Test
    void getClubsByFilter_withFilters_appliesAllFilters() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Club> clubPage = new PageImpl<>(List.of(testClub), pageable, 1);

        when(clubRepository.getAllClubsByFilter(null, testCampusId, testCategoryId, "ACTIVE", pageable))
                .thenReturn(clubPage);
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);

        // Act
        PageResponse<ClubManagementResponse> result = clubService.getClubsByFilter(
                null, testCampusId, testCategoryId, "ACTIVE", pageable, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getPageNumber());

        verify(clubRepository, times(1)).getAllClubsByFilter(null, testCampusId, testCategoryId, "ACTIVE", pageable);
        verify(clubRepository, times(1)).countMembersByClubId(testClubId);
    }

    // ================= createClub Tests =================

    @Test
    void createClub_asStaff_validRequest_returnsClubManagementResponse() throws AppException {
        // Arrange
        CreateClubRequest request = new CreateClubRequest();
        request.setClubCode("CLUB002");
        request.setClubName("New Club");
        request.setDescription("Description");
        request.setCampusId(testCampusId);
        request.setCategoryId(testCategoryId);
        request.setPresidentEmail(testEmail);

        Map<String, Object> epuProfile = new HashMap<>();
        epuProfile.put("email", testEmail);
        epuProfile.put("fullName", "Test User");
        epuProfile.put("studentCode", "SE123456");

        SystemRole clubOfficerRole = new SystemRole();
        clubOfficerRole.setRoleName("CLUB_OFFICER");

        when(clubRepository.findByClubCode("CLUB002")).thenReturn(Optional.empty());
        when(clubRepository.findByClubName("New Club")).thenReturn(Optional.empty());
        when(campusRepository.findById(testCampusId)).thenReturn(Optional.of(testCampus));
        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(epuApiService.findProfileByEmail(testEmail)).thenReturn(Optional.of(epuProfile));
        when(userRepository.findByEmailIgnoreCase(testEmail)).thenReturn(Optional.of(testUser));
        when(clubRepository.save(any(Club.class))).thenReturn(testClub);
        when(systemRoleRepository.findByRoleName(anyString())).thenReturn(Optional.of(clubOfficerRole));
        when(clubRoleRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        when(clubMemberShipRepository.save(any(ClubMemberShip.class))).thenReturn(new ClubMemberShip());
        when(clubRoleRepository.findByClubIdAndRoleCode(anyLong(), eq("CLUB_PRESIDENT")))
                .thenReturn(Optional.of(new ClubRole()));
        when(roleMemberShipRepository.save(any(RoleMemberShip.class))).thenReturn(new RoleMemberShip());
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(1L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(0L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(0L);

        // Mock  notification - không throw exception
        doNothing().when(notificationService).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationPriority.class),
                anyString(), anyLong(), any(), any(), any()
        );

        // Act
        ClubManagementResponse result = clubService.createClub(request, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(testClubId, result.getId());

        verify(clubRepository, times(1)).save(any(Club.class));
        verify(clubRoleRepository, times(1)).saveAll(anyList());

        // Verify async notification was sent
        verify(notificationService, times(1)).sendToUsers(
                eq(List.of(testUser.getId())),
                eq(testStaffId),
                contains("Chủ nhiệm CLB"),
                contains(testClubName),
                eq(NotificationType.CLUB_ROLE_ASSIGNED),
                eq(NotificationPriority.HIGH),
                anyString(),
                eq(testClubId),
                isNull(),
                isNull(),
                isNull()
        );
    }


    @Test
    void createClub_duplicateClubCode_throwsAppException() {
        // Arrange
        CreateClubRequest request = new CreateClubRequest();
        request.setClubCode(testClubCode);

        when(clubRepository.findByClubCode(testClubCode)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.createClub(request, testStaffId));
        assertEquals(ErrorCode.CLUB_CODE_EXISTED, exception.getErrorCode());

        verify(clubRepository, never()).save(any());
    }

    @Test
    void createClub_duplicateClubName_throwsAppException() {
        // Arrange
        CreateClubRequest request = new CreateClubRequest();
        request.setClubCode("CLUB002");
        request.setClubName(testClubName);

        when(clubRepository.findByClubCode("CLUB002")).thenReturn(Optional.empty());
        when(clubRepository.findByClubName(testClubName)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.createClub(request, testStaffId));
        assertEquals(ErrorCode.CLUB_NAME_EXISTED, exception.getErrorCode());

        verify(clubRepository, never()).save(any());
    }

    @Test
    void createClub_invalidPresidentEmail_throwsAppException() {
        // Arrange
        CreateClubRequest request = new CreateClubRequest();
        request.setClubCode("CLUB002");
        request.setClubName("New Club");
        request.setCampusId(testCampusId);
        request.setCategoryId(testCategoryId);
        request.setPresidentEmail("invalid@email.com");

        when(clubRepository.findByClubCode("CLUB002")).thenReturn(Optional.empty());
        when(clubRepository.findByClubName("New Club")).thenReturn(Optional.empty());
        when(campusRepository.findById(testCampusId)).thenReturn(Optional.of(testCampus));
        when(clubCategoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(epuApiService.findProfileByEmail("invalid@email.com")).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.createClub(request, testStaffId));
        assertEquals(ErrorCode.USER_NOT_VALID, exception.getErrorCode());

        verify(clubRepository, never()).save(any());
    }

    // ================= updateClub Tests =================

    @Test
    void updateClub_validRequest_returnsUpdatedClub() throws AppException {
        // Arrange
        UpdateClubRequest request = new UpdateClubRequest();
        request.setClubName("Updated Club Name");
        request.setDescription("Updated Description");

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.findByClubName("Updated Club Name")).thenReturn(Optional.empty());
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);

        // Act
        ClubManagementResponse result = clubService.updateClub(testClubId, request, testStaffId);

        // Assert
        assertNotNull(result);
        verify(clubRepository, times(1)).save(testClub);
    }


    @Test
    void updateClub_nonExistingClub_throwsAppException() {
        // Arrange
        UpdateClubRequest request = new UpdateClubRequest();
        when(clubRepository.findById(testClubId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.updateClub(testClubId, request, testStaffId));
        assertEquals(ErrorCode.CLUB_NOT_FOUND, exception.getErrorCode());

        verify(clubRepository, never()).save(any());
    }

    // ================= deactivateClub Tests =================

    @Test
    void deactivateClub_validClub_deactivatesSuccessfully() throws AppException {
        // Arrange
        ClubMemberShip membership1 = new ClubMemberShip();
        membership1.setUser(testUser);

        User user2 = User.builder().id(3L).email("user2@test.com").build();
        ClubMemberShip membership2 = new ClubMemberShip();
        membership2.setUser(user2);

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubMemberShipRepository.findByClubIdAndStatus(testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(List.of(membership1, membership2));

        // Mock  notification
        doNothing().when(notificationService).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationPriority.class),
                anyString(), anyLong(), any(), any(), any()
        );

        // Act
        assertDoesNotThrow(() -> clubService.deactivateClub(testClubId, testStaffId));

        // Assert
        assertEquals("UNACTIVE", testClub.getStatus());
        verify(clubRepository, times(1)).save(testClub);

        // Verify notification sent to all members
        verify(notificationService, times(1)).sendToUsers(
                eq(List.of(testUser.getId(), user2.getId())),
                eq(testStaffId),
                contains("vô hiệu hóa"),
                contains(testClubName),
                eq(NotificationType.SYSTEM_WARNING),
                eq(NotificationPriority.HIGH),
                anyString(),
                eq(testClubId),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void deactivateClub_noActiveMembers_sendsNoNotification() throws AppException {
        // Arrange
        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubMemberShipRepository.findByClubIdAndStatus(testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> clubService.deactivateClub(testClubId, testStaffId));

        // Assert
        assertEquals("UNACTIVE", testClub.getStatus());
        verify(clubRepository, times(1)).save(testClub);

        // Verify no notification sent when no active members
        verify(notificationService, never()).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(),
                any(), any(), anyString(), anyLong(), any(), any(), any()
        );
    }


    // ================= activateClub Tests =================

    @Test
    void activateClub_validClub_activatesSuccessfully() throws AppException {
        // Arrange
        testClub.setStatus("UNACTIVE");

        ClubMemberShip membership1 = new ClubMemberShip();
        membership1.setUser(testUser);

        User user2 = User.builder().id(3L).email("user2@test.com").build();
        ClubMemberShip membership2 = new ClubMemberShip();
        membership2.setUser(user2);

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubMemberShipRepository.findByClubIdAndStatus(testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(List.of(membership1, membership2));

        // Mock  notification
        doNothing().when(notificationService).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(),
                any(NotificationType.class), any(NotificationPriority.class),
                anyString(), anyLong(), any(), any(), any()
        );

        // Act
        assertDoesNotThrow(() -> clubService.activateClub(testClubId, testStaffId));

        // Assert
        assertEquals("ACTIVE", testClub.getStatus());
        verify(clubRepository, times(1)).save(testClub);

        // Verify notification sent to all members
        verify(notificationService, times(1)).sendToUsers(
                eq(List.of(testUser.getId(), user2.getId())),
                eq(testStaffId),
                contains("kích hoạt"),
                contains(testClubName),
                eq(NotificationType.SYSTEM_ANNOUNCEMENT),
                eq(NotificationPriority.HIGH),
                anyString(),
                eq(testClubId),
                isNull(),
                isNull(),
                isNull()
        );
    }

    @Test
    void activateClub_noActiveMembers_sendsNoNotification() throws AppException {
        // Arrange
        testClub.setStatus("UNACTIVE");
        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubMemberShipRepository.findByClubIdAndStatus(testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(Collections.emptyList());

        // Act
        assertDoesNotThrow(() -> clubService.activateClub(testClubId, testStaffId));

        // Assert
        assertEquals("ACTIVE", testClub.getStatus());
        verify(clubRepository, times(1)).save(testClub);

        // Verify no notification sent when no active members
        verify(notificationService, never()).sendToUsers(
                anyList(), anyLong(), anyString(), anyString(),
                any(), any(), anyString(), anyLong(), any(), any(), any()
        );
    }


    // ================= getClubForManagement Tests =================

    @Test
    void getClubForManagement_existingClub_returnsClubManagementResponse() throws AppException {
        // Arrange
        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubManagementResponse(testClub)).thenReturn(testClubManagementResponse);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(15L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(8L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(4L);

        // Act
        ClubManagementResponse result = clubService.getClubForManagement(testClubId, testStaffId);

        // Assert
        assertNotNull(result);
        assertEquals(testClubId, result.getId());
        assertEquals(15L, result.getTotalMembers());
        assertEquals(8L, result.getTotalEvents());
        assertEquals(4L, result.getTotalPosts());
    }

    @Test
    void getClubForManagement_nonExistingClub_throwsAppException() {
        // Arrange
        when(clubRepository.findById(testClubId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () ->
                clubService.getClubForManagement(testClubId, testStaffId));

        verify(clubRepository, times(1)).findById(testClubId);
    }

    // ================= getClubInfo Tests =================

    @Test
    void getClubInfo_asMember_existingClub_returnsClubDetailData() throws AppException {
        // Arrange
        when(clubMemberShipRepository.existsByUserIdAndClubIdAndStatus(testUserId, testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(true);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(true);

        // Act
        ClubDetailData result = clubService.getClubInfo(testClubId, testUserId);

        // Assert
        assertNotNull(result);
        assertEquals(testClubId, result.getId());

        verify(clubMemberShipRepository, times(1))
                .existsByUserIdAndClubIdAndStatus(testUserId, testClubId, ClubMemberShipStatus.ACTIVE);
    }

    @Test
    void getClubInfo_asNonMember_throwsAppException() {
        // Arrange
        when(clubMemberShipRepository.existsByUserIdAndClubIdAndStatus(testUserId, testClubId, ClubMemberShipStatus.ACTIVE))
                .thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.getClubInfo(testClubId, testUserId));
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());

        verify(clubRepository, never()).findByIdWithDetails(any());
    }

    // ================= updateClubInfo Tests =================

    @Test
    void updateClubInfo_validRequest_returnsUpdatedClubInfo() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");
        request.setEmail("newemail@fpt.edu.vn");

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, null, null);

        // Assert
        assertNotNull(result);
        verify(clubRepository, times(1)).save(testClub);
    }



    @Test
    void updateClubInfo_inactiveClub_throwsAppException() {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();

        // Create inactive club
        Club inactiveClub = Club.builder()
                .id(testClubId)
                .clubCode(testClubCode)
                .clubName(testClubName)
                .status("UNACTIVE")  // Set inactive
                .campus(testCampus)
                .clubCategory(testCategory)
                .build();

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(inactiveClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.updateClubInfo(testClubId, request, testUserId, null, null));
        assertEquals(ErrorCode.CLUB_NOT_ACTIVE, exception.getErrorCode());

        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateClubInfo_duplicateClubCode_throwsRuntimeException() {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setClubCode("EXISTING_CODE");

        Club existingClub = Club.builder()
                .id(999L)
                .clubCode("EXISTING_CODE")
                .build();

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.findByClubCode("EXISTING_CODE")).thenReturn(Optional.of(existingClub));

        // Act & Assert - ifPresent lambda throws RuntimeException directly
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                clubService.updateClubInfo(testClubId, request, testUserId, null, null));
        assertEquals("CLUB_CODE_EXISTED", exception.getMessage());
    }

    @Test
    void updateClubInfo_duplicateClubName_throwsRuntimeException() {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setClubName("Existing Name");

        Club existingClub = Club.builder()
                .id(999L)
                .clubName("Existing Name")
                .build();

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.findByClubName("Existing Name")).thenReturn(Optional.of(existingClub));

        // Act & Assert - ifPresent lambda throws RuntimeException directly
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                clubService.updateClubInfo(testClubId, request, testUserId, null, null));
        assertEquals("CLUB_NAME_EXISTED", exception.getMessage());
    }

    @Test
    void updateClubInfo_validLogoUpload_success() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");

        // Create a mock file under 10MB (e.g., 5MB)
        byte[] content = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile logoFile = new MockMultipartFile(
                "logoFile",
                "logo.png",
                "image/png",
                content
        );

        CloudinaryService.UploadResult uploadResult = new CloudinaryService.UploadResult(
                "https://cloudinary.com/logo.png",
                "public_id_logo",
                "png",
                5L * 1024L * 1024L
        );

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("club/logos")))
                .thenReturn(uploadResult);
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, logoFile, null);

        // Assert
        assertNotNull(result);
        verify(cloudinaryService, times(1)).uploadImage(any(MultipartFile.class), eq("club/logos"));
        verify(clubRepository, times(1)).save(testClub);
        assertEquals("https://cloudinary.com/logo.png", testClub.getLogoUrl());
    }

    @Test
    void updateClubInfo_logoFileTooLarge_throwsAppException() {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");

        // Create a mock file over 10MB (e.g., 15MB)
        byte[] content = new byte[15 * 1024 * 1024]; // 15MB
        MockMultipartFile logoFile = new MockMultipartFile(
                "logoFile",
                "logo.png",
                "image/png",
                content
        );

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.updateClubInfo(testClubId, request, testUserId, logoFile, null));

        assertEquals(ErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("10MB"));
        verify(cloudinaryService, never()).uploadImage(any(), any());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateClubInfo_validBannerUpload_success() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");

        // Create a mock file under 10MB (e.g., 8MB)
        byte[] content = new byte[8 * 1024 * 1024]; // 8MB
        MockMultipartFile bannerFile = new MockMultipartFile(
                "bannerFile",
                "banner.png",
                "image/png",
                content
        );

        CloudinaryService.UploadResult uploadResult = new CloudinaryService.UploadResult(
                "https://cloudinary.com/banner.png",
                "public_id_banner",
                "png",
                8L * 1024L * 1024L
        );

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(cloudinaryService.uploadImage(any(MultipartFile.class), eq("club/banners")))
                .thenReturn(uploadResult);
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, null, bannerFile);

        // Assert
        assertNotNull(result);
        verify(cloudinaryService, times(1)).uploadImage(any(MultipartFile.class), eq("club/banners"));
        verify(clubRepository, times(1)).save(testClub);
        assertEquals("https://cloudinary.com/banner.png", testClub.getBannerUrl());
    }

    @Test
    void updateClubInfo_bannerFileTooLarge_throwsAppException() {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");

        // Create a mock file over 10MB (e.g., 20MB)
        byte[] content = new byte[20 * 1024 * 1024]; // 20MB
        MockMultipartFile bannerFile = new MockMultipartFile(
                "bannerFile",
                "banner.png",
                "image/png",
                content
        );

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
                clubService.updateClubInfo(testClubId, request, testUserId, null, bannerFile));

        assertEquals(ErrorCode.FILE_TOO_LARGE, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("10MB"));
        verify(cloudinaryService, never()).uploadImage(any(), any());
        verify(clubRepository, never()).save(any());
    }

    @Test
    void updateClubInfo_removeLogo_success() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setRemoveLogo(true);
        testClub.setLogoUrl("https://cloudinary.com/old-logo.png");

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, null, null);

        // Assert
        assertNotNull(result);
        assertNull(testClub.getLogoUrl());
        verify(clubRepository, times(1)).save(testClub);
    }

    @Test
    void updateClubInfo_removeBanner_success() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setRemoveBanner(true);
        testClub.setBannerUrl("https://cloudinary.com/old-banner.png");

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, null, null);

        // Assert
        assertNotNull(result);
        assertNull(testClub.getBannerUrl());
        verify(clubRepository, times(1)).save(testClub);
    }

    @Test
    void updateClubInfo_uploadBothLogoAndBanner_success() throws AppException {
        // Arrange
        UpdateClubInfoRequest request = new UpdateClubInfoRequest();
        request.setDescription("Updated description");

        // Create mock files under 10MB
        byte[] logoContent = new byte[5 * 1024 * 1024]; // 5MB
        byte[] bannerContent = new byte[8 * 1024 * 1024]; // 8MB
        MockMultipartFile logoFile = new MockMultipartFile("logoFile", "logo.png", "image/png", logoContent);
        MockMultipartFile bannerFile = new MockMultipartFile("bannerFile", "banner.png", "image/png", bannerContent);

        CloudinaryService.UploadResult logoUploadResult = new CloudinaryService.UploadResult(
                "https://cloudinary.com/logo.png", "public_id_logo", "png", 5L * 1024L * 1024L);
        CloudinaryService.UploadResult bannerUploadResult = new CloudinaryService.UploadResult(
                "https://cloudinary.com/banner.png", "public_id_banner", "png", 8L * 1024L * 1024L);

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(cloudinaryService.uploadImage(eq(logoFile), eq("club/logos"))).thenReturn(logoUploadResult);
        when(cloudinaryService.uploadImage(eq(bannerFile), eq("club/banners"))).thenReturn(bannerUploadResult);
        when(clubRepository.save(testClub)).thenReturn(testClub);
        when(clubRepository.findByIdWithDetails(testClubId)).thenReturn(Optional.of(testClub));
        when(clubMapper.toClubDetailData(testClub)).thenReturn(testClubDetailData);
        when(clubRepository.countMembersByClubId(testClubId)).thenReturn(10L);
        when(clubRepository.countEventsByClubId(testClubId)).thenReturn(5L);
        when(clubRepository.countNewsByClubId(testClubId)).thenReturn(3L);
        when(clubRepository.hasActiveRecruitment(testClubId)).thenReturn(false);

        // Act
        ClubDetailData result = clubService.updateClubInfo(testClubId, request, testUserId, logoFile, bannerFile);

        // Assert
        assertNotNull(result);
        verify(cloudinaryService, times(1)).uploadImage(eq(logoFile), eq("club/logos"));
        verify(cloudinaryService, times(1)).uploadImage(eq(bannerFile), eq("club/banners"));
        verify(clubRepository, times(1)).save(testClub);
        assertEquals("https://cloudinary.com/logo.png", testClub.getLogoUrl());
        assertEquals("https://cloudinary.com/banner.png", testClub.getBannerUrl());
    }
}

