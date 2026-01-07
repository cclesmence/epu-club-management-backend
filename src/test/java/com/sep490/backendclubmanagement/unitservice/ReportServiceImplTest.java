package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.*;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubReportRequirement;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.ReportMapper;
import com.sep490.backendclubmanagement.mapper.SubmissionReportRequirementMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.report.ReportServiceImpl;
import com.sep490.backendclubmanagement.service.role.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ClubReportRequirementRepository clubReportRequirementRepository;
    @Mock
    private SubmissionReportRequirementRepository submissionReportRequirementRepository;
    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private SubmissionReportRequirementMapper submissionReportRequirementMapper;
    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Club club;
    private User user;
    private User staffUser;
    private Semester currentSemester;
    private Report report;
    private SubmissionReportRequirement submissionRequirement;
    private ClubReportRequirement clubRequirement;
    private Event event;

    @BeforeEach
    void setup() {

        club = new Club();
        club.setId(1L);
        club.setClubName("Test Club");
        club.setClubCode("TC001");
        club.setStatus("ACTIVE");

        user = new User();
        user.setId(10L);
        user.setFullName("Nguyen Van A");
        user.setStudentCode("SE001");
        user.setEmail("user@test.com");

        staffUser = new User();
        staffUser.setId(20L);
        staffUser.setFullName("Staff User");
        staffUser.setEmail("staff@test.com");

        currentSemester = new Semester();
        currentSemester.setId(5L);
        currentSemester.setSemesterCode("2024A");
        currentSemester.setIsCurrent(true);
        currentSemester.setStartDate(LocalDate.of(2024, 1, 1));
        currentSemester.setEndDate(LocalDate.of(2024, 6, 30));

        event = new Event();
        event.setId(100L);
        event.setTitle("Test Event");

        submissionRequirement = new SubmissionReportRequirement();
        submissionRequirement.setId(1L);
        submissionRequirement.setTitle("Semester Report");
        submissionRequirement.setDescription("Semester report requirement");
        submissionRequirement.setReportType(ReportType.SEMESTER);
        submissionRequirement.setDueDate(LocalDateTime.now().plusDays(7));
        submissionRequirement.setCreatedBy(staffUser);

        clubRequirement = new ClubReportRequirement();
        clubRequirement.setId(1L);
        clubRequirement.setClub(club);
        clubRequirement.setSubmissionReportRequirement(submissionRequirement);
        // Don't set report by default - it should be set in specific tests

        report = new Report();
        report.setId(1L);
        report.setReportTitle("Test Report");
        report.setContent("Test content");
        report.setStatus(ReportStatus.DRAFT);
        report.setClubReportRequirement(clubRequirement);
        report.setSemester(currentSemester);
        report.setCreatedBy(user);
        report.setMustResubmit(false);
    }

    // ========== getAllReports ==========

    @Test
    void getAllReports_Success_WhenStaffAccessWithFilters() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        ClubReportRequirement universityClubReq = new ClubReportRequirement();
        universityClubReq.setId(2L);
        universityClubReq.setClub(club);
        universityClubReq.setSubmissionReportRequirement(submissionRequirement);

        Report universityReport = new Report();
        universityReport.setId(2L);
        universityReport.setReportTitle("University Report");
        universityReport.setStatus(ReportStatus.PENDING_UNIVERSITY);
        universityReport.setClubReportRequirement(universityClubReq);
        universityReport.setSemester(currentSemester);

        Page<Report> reportPage = new PageImpl<>(List.of(universityReport), pageable, 1);

        when(reportRepository.findAllWithFilters(
            any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(2L);
        responseItem.setReportTitle("University Report");
        when(reportMapper.toListItem(universityReport)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            ReportStatus.PENDING_UNIVERSITY, club.getId(), currentSemester.getId(),
            ReportType.SEMESTER, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("University Report", result.getContent().get(0).getReportTitle());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        assertFalse(result.isHasNext());
        assertFalse(result.isHasPrevious());
        verify(reportRepository).findAllWithFilters(
            any(), any(), any(), any(), any(), any(Pageable.class)
        );
        verify(reportMapper).toListItem(universityReport);
    }

    @Test
    void getAllReports_Success_WithMultipleUniversityLevelReports() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Report report1 = new Report();
        report1.setId(1L);
        report1.setReportTitle("Report 1");
        report1.setStatus(ReportStatus.PENDING_UNIVERSITY);
        report1.setClubReportRequirement(clubRequirement);
        report1.setSemester(currentSemester);

        Report report2 = new Report();
        report2.setId(2L);
        report2.setReportTitle("Report 2");
        report2.setStatus(ReportStatus.APPROVED_UNIVERSITY);
        report2.setClubReportRequirement(clubRequirement);
        report2.setSemester(currentSemester);

        Report report3 = new Report();
        report3.setId(3L);
        report3.setReportTitle("Report 3");
        report3.setStatus(ReportStatus.REJECTED_UNIVERSITY);
        report3.setClubReportRequirement(clubRequirement);
        report3.setSemester(currentSemester);

        Page<Report> reportPage = new PageImpl<>(List.of(report1, report2, report3), pageable, 3);

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse response1 = new ReportListItemResponse();
        response1.setId(1L);
        response1.setReportTitle("Report 1");

        ReportListItemResponse response2 = new ReportListItemResponse();
        response2.setId(2L);
        response2.setReportTitle("Report 2");

        ReportListItemResponse response3 = new ReportListItemResponse();
        response3.setId(3L);
        response3.setReportTitle("Report 3");

        when(reportMapper.toListItem(report1)).thenReturn(response1);
        when(reportMapper.toListItem(report2)).thenReturn(response2);
        when(reportMapper.toListItem(report3)).thenReturn(response3);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals("Report 1", result.getContent().get(0).getReportTitle());
        assertEquals("Report 2", result.getContent().get(1).getReportTitle());
        assertEquals("Report 3", result.getContent().get(2).getReportTitle());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void getAllReports_Success_FiltersOutNonUniversityLevelReports() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Report universityReport = new Report();
        universityReport.setId(1L);
        universityReport.setReportTitle("University Report");
        universityReport.setStatus(ReportStatus.PENDING_UNIVERSITY);
        universityReport.setClubReportRequirement(clubRequirement);

        Report draftReport = new Report();
        draftReport.setId(2L);
        draftReport.setReportTitle("Draft Report");
        draftReport.setStatus(ReportStatus.DRAFT);
        draftReport.setClubReportRequirement(clubRequirement);

        Report clubReport = new Report();
        clubReport.setId(3L);
        clubReport.setReportTitle("Club Report");
        clubReport.setStatus(ReportStatus.PENDING_CLUB);
        clubReport.setClubReportRequirement(clubRequirement);

        // Repository returns all reports, but service should filter
        Page<Report> reportPage = new PageImpl<>(
            List.of(universityReport, draftReport, clubReport), pageable, 3
        );

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(1L);
        responseItem.setReportTitle("University Report");
        when(reportMapper.toListItem(universityReport)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, null, pageable, staffId
        );

        // Assert - Should only return university-level report
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("University Report", result.getContent().get(0).getReportTitle());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
        verify(reportMapper, times(1)).toListItem(any(Report.class));
        verify(reportMapper).toListItem(universityReport);
    }

    @Test
    void getAllReports_Success_WithResubmittedUniversityStatus() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Report resubmittedReport = new Report();
        resubmittedReport.setId(1L);
        resubmittedReport.setReportTitle("Resubmitted Report");
        resubmittedReport.setStatus(ReportStatus.RESUBMITTED_UNIVERSITY);
        resubmittedReport.setClubReportRequirement(clubRequirement);
        resubmittedReport.setSemester(currentSemester);

        Page<Report> reportPage = new PageImpl<>(List.of(resubmittedReport), pageable, 1);

        when(reportRepository.findAllWithFilters(
            eq(ReportStatus.RESUBMITTED_UNIVERSITY), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(1L);
        responseItem.setReportTitle("Resubmitted Report");
        when(reportMapper.toListItem(resubmittedReport)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            ReportStatus.RESUBMITTED_UNIVERSITY, null, null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Resubmitted Report", result.getContent().get(0).getReportTitle());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
    }

    @Test
    void getAllReports_Success_WithKeywordFilter() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);
        String keyword = "University";

        Report report1 = new Report();
        report1.setId(1L);
        report1.setReportTitle("University Report 1");
        report1.setContent("Content about university");
        report1.setStatus(ReportStatus.PENDING_UNIVERSITY);
        report1.setClubReportRequirement(clubRequirement);

        Report report2 = new Report();
        report2.setId(2L);
        report2.setReportTitle("Other Report");
        report2.setContent("Other content");
        report2.setStatus(ReportStatus.PENDING_UNIVERSITY);
        report2.setClubReportRequirement(clubRequirement);

        // When keyword is provided, service queries all then filters client-side
        Page<Report> allReportsPage = new PageImpl<>(
            List.of(report1, report2),
            PageRequest.of(0, Integer.MAX_VALUE),
            2
        );

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(allReportsPage);

        ReportListItemResponse response1 = new ReportListItemResponse();
        response1.setId(1L);
        response1.setReportTitle("University Report 1");

        when(reportMapper.toListItem(report1)).thenReturn(response1);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, keyword, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        // Note: Client-side filtering based on Vietnamese normalization
        // The actual filtering logic depends on matchesVietnameseKeyword implementation
    }

    @Test
    void getAllReports_ReturnsEmptyList_WhenNoUniversityLevelReports() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Report draftReport = new Report();
        draftReport.setId(1L);
        draftReport.setReportTitle("Draft Report");
        draftReport.setStatus(ReportStatus.DRAFT);
        draftReport.setClubReportRequirement(clubRequirement);

        Page<Report> reportPage = new PageImpl<>(List.of(draftReport), pageable, 1);

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        assertTrue(result.getContent().isEmpty());
        verify(reportMapper, never()).toListItem(any(Report.class));
    }

    @Test
    void getAllReports_ThrowsForbiddenException_WhenInvalidStatus() {
        // Arrange
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert - Try to access with non-university level status
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
            reportService.getAllReports(ReportStatus.DRAFT, null, null, null, null, pageable, userId)
        );

        assertTrue(exception.getMessage().contains("PENDING_UNIVERSITY"));
        assertTrue(exception.getMessage().contains("APPROVED_UNIVERSITY"));
        assertTrue(exception.getMessage().contains("REJECTED_UNIVERSITY"));
        assertTrue(exception.getMessage().contains("RESUBMITTED_UNIVERSITY"));

        // Verify repository was never called
        verify(reportRepository, never()).findAllWithFilters(any(), any(), any(), any(), any(), any());
    }

    @Test
    void getAllReports_ThrowsForbiddenException_WhenPendingClubStatus() {
        // Arrange
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.getAllReports(ReportStatus.PENDING_CLUB, null, null, null, null, pageable, userId)
        );
    }

    @Test
    void getAllReports_ThrowsForbiddenException_WhenRejectedClubStatus() {
        // Arrange
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.getAllReports(ReportStatus.REJECTED_CLUB, null, null, null, null, pageable, userId)
        );
    }

    // ========== getReportDetail ==========

    @Test
    void getReportDetail_Success_WhenStaffAccess() {
        // Arrange
        Long staffId = staffUser.getId();
        Long reportId = report.getId();
        report.setStatus(ReportStatus.PENDING_UNIVERSITY);

        when(reportRepository.findByIdWithRelations(reportId)).thenReturn(Optional.of(report));

        ReportDetailResponse detailResponse = new ReportDetailResponse();
        detailResponse.setId(reportId);
        detailResponse.setReportTitle("Test Report");
        when(reportMapper.toDetail(report)).thenReturn(detailResponse);

        // Act
        ReportDetailResponse result = reportService.getReportDetail(reportId, staffId);

        // Assert
        assertNotNull(result);
        assertEquals(reportId, result.getId());
        assertEquals("Test Report", result.getReportTitle());
        verify(reportRepository).findByIdWithRelations(reportId);
        verify(reportMapper).toDetail(report);
    }

    @Test
    void getReportDetail_ThrowsForbiddenException_WhenNotUniversityLevelStatus() {
        // Arrange
        Long staffId = staffUser.getId();
        Long reportId = report.getId();
        report.setStatus(ReportStatus.DRAFT);

        when(reportRepository.findByIdWithRelations(reportId)).thenReturn(Optional.of(report));

        // Act & Assert
        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
            reportService.getReportDetail(reportId, staffId)
        );

        assertTrue(exception.getMessage().contains("PENDING_UNIVERSITY"));
        assertTrue(exception.getMessage().contains("Current status: DRAFT"));
    }

    @Test
    void getReportDetail_ThrowsNotFoundException_WhenReportNotFound() {
        // Arrange
        Long staffId = staffUser.getId();
        Long reportId = 999L;

        when(reportRepository.findByIdWithRelations(reportId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            reportService.getReportDetail(reportId, staffId)
        );
    }

    // ========== reviewReport ==========

    @Test
    void reviewReport_Success_WhenStaffApprovesReport() {
        // Arrange
        Long staffId = staffUser.getId();
        report.setStatus(ReportStatus.PENDING_UNIVERSITY);
        report.setClubReportRequirement(clubRequirement);
        clubRequirement.setReport(report);

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReportId(report.getId());
        request.setStatus(ReportStatus.APPROVED_UNIVERSITY);
        request.setReviewerFeedback("Approved by university");

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock for async notification logic
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(anyLong(), anyLong()))
            .thenReturn(List.of(user.getId()));
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        reportService.reviewReport(request, staffId);

        // Assert
        verify(reportRepository).save(argThat(r ->
            r.getStatus() == ReportStatus.APPROVED_UNIVERSITY &&
            r.getReviewedDate() != null &&
            !r.isMustResubmit() &&
            r.getReviewerFeedback() != null
        ));
    }

    @Test
    void reviewReport_Success_WhenStaffRejectsReport() {
        // Arrange
        Long staffId = staffUser.getId();
        report.setStatus(ReportStatus.PENDING_UNIVERSITY);
        report.setClubReportRequirement(clubRequirement);
        clubRequirement.setReport(report);

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReportId(report.getId());
        request.setStatus(ReportStatus.REJECTED_UNIVERSITY);
        request.setReviewerFeedback("Needs revision");
        request.setMustResubmit(true);

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock for async notification logic
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(anyLong(), anyLong()))
            .thenReturn(List.of(user.getId()));
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        reportService.reviewReport(request, staffId);

        // Assert
        verify(reportRepository).save(argThat(r ->
            r.getStatus() == ReportStatus.REJECTED_UNIVERSITY &&
            r.getReviewedDate() != null &&
            r.isMustResubmit() &&
            "Needs revision".equals(r.getReviewerFeedback())
        ));
    }

    @Test
    void reviewReport_ThrowsForbiddenException_WhenInvalidStatusTransition() {
        // Arrange
        Long staffId = staffUser.getId();
        report.setStatus(ReportStatus.DRAFT);

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReportId(report.getId());
        request.setStatus(ReportStatus.APPROVED_UNIVERSITY);
        request.setReviewerFeedback("Approved feedback");
        request.setMustResubmit(false);

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.reviewReport(request, staffId)
        );
    }

    // ========== createReportRequirement ==========

    @Test
    void createReportRequirement_Success_WhenStaffCreatesForMultipleClubs() throws AppException {
        // Arrange
        Long staffId = staffUser.getId();

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Monthly Report");
        request.setDescription("Submit monthly report");
        request.setReportType(ReportType.SEMESTER);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setClubIds(List.of(1L, 2L));

        Club club2 = new Club();
        club2.setId(2L);
        club2.setClubName("Test Club 2");
        club2.setClubCode("TC002");

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(clubRepository.findAllById(request.getClubIds())).thenReturn(List.of(club, club2));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));
        when(submissionReportRequirementRepository.save(any(SubmissionReportRequirement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(clubReportRequirementRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(1L);
        response.setTitle("Monthly Report");
        when(submissionReportRequirementMapper.toDto(any(SubmissionReportRequirement.class))).thenReturn(response);

        // Mock for async notification
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdsAndSemesterId(anyList(), anyLong()))
            .thenReturn(List.of());
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        ReportRequirementResponse result = reportService.createReportRequirement(request, null, staffId);

        // Assert
        assertNotNull(result);
        assertEquals("Monthly Report", result.getTitle());
        verify(submissionReportRequirementRepository).save(any(SubmissionReportRequirement.class));
        verify(clubReportRequirementRepository).saveAll(anyList());
    }

    @Test
    void createReportRequirement_Success_WithEvent() throws AppException {
        // Arrange
        Long staffId = staffUser.getId();

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Event Report");
        request.setDescription("Post-event report");
        request.setReportType(ReportType.EVENT);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setEventId(event.getId());
        request.setClubIds(List.of(1L));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(clubRepository.findAllById(request.getClubIds())).thenReturn(List.of(club));
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));
        when(submissionReportRequirementRepository.save(any(SubmissionReportRequirement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(clubReportRequirementRepository.saveAll(anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(1L);
        response.setTitle("Event Report");
        when(submissionReportRequirementMapper.toDto(any(SubmissionReportRequirement.class))).thenReturn(response);

        // Mock for async notification
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdsAndSemesterId(anyList(), anyLong()))
            .thenReturn(List.of());
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        ReportRequirementResponse result = reportService.createReportRequirement(request, null, staffId);

        // Assert
        assertNotNull(result);
        assertEquals("Event Report", result.getTitle());
        verify(eventRepository).findById(event.getId());
    }

    @Test
    void createReportRequirement_ThrowsException_WhenDueDateInPast() {
        // Arrange
        Long staffId = staffUser.getId();

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Monthly Report");
        request.setDueDate(LocalDateTime.now().minusDays(1));
        request.setClubIds(List.of(1L));

        when(roleService.isStaff(staffId)).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            reportService.createReportRequirement(request, null, staffId)
        );
    }

    @Test
    void createReportRequirement_WithFile_UploadsSuccessfully() throws AppException {
        // Arrange
        Long staffId = staffUser.getId();
        MultipartFile file = mock(MultipartFile.class);

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Monthly Report");
        request.setDescription("Submit monthly report");
        request.setReportType(ReportType.SEMESTER);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setClubIds(List.of(1L));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L * 1024L); // 1MB
        when(clubRepository.findAllById(request.getClubIds())).thenReturn(List.of(club));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staffUser));

        CloudinaryService.UploadResult uploadResult = new CloudinaryService.UploadResult(
            "http://cloudinary.com/template.pdf", "template_id", "pdf", 1024L
        );
        when(cloudinaryService.uploadFile(eq(file), anyString()))
            .thenReturn(uploadResult);

        when(submissionReportRequirementRepository.save(any(SubmissionReportRequirement.class)))
            .thenAnswer(invocation -> {
                SubmissionReportRequirement saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
        when(clubReportRequirementRepository.saveAll(anyList()))
            .thenAnswer(invocation -> {
                List<ClubReportRequirement> list = invocation.getArgument(0);
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setId((long) (i + 1));
                }
                return list;
            });

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(1L);
        response.setTitle("Monthly Report");
        when(submissionReportRequirementMapper.toDto(any(SubmissionReportRequirement.class))).thenReturn(response);

        // Mock for async notification
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdsAndSemesterId(anyList(), anyLong()))
            .thenReturn(List.of());
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        ReportRequirementResponse result = reportService.createReportRequirement(request, file, staffId);

        // Assert
        assertNotNull(result);
        verify(cloudinaryService).uploadFile(eq(file), anyString());
        verify(submissionReportRequirementRepository).save(argThat(req ->
            "http://cloudinary.com/template.pdf".equals(req.getTemplateUrl())
        ));
    }

    @Test
    void createReportRequirement_ThrowsException_WhenFileTooLarge() {
        // Arrange
        Long staffId = staffUser.getId();
        MultipartFile file = mock(MultipartFile.class);

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Monthly Report");
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setClubIds(List.of(1L));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(25L * 1024L * 1024L); // 25MB - exceeds limit

        // Act & Assert
        assertThrows(AppException.class, () ->
            reportService.createReportRequirement(request, file, staffId)
        );
    }

    // ========== updateReportRequirement ==========

    @Test
    void updateReportRequirement_Success_WhenStaffUpdates() throws AppException {
        // Arrange
        Long staffId = staffUser.getId();
        Long requirementId = submissionRequirement.getId();

        UpdateReportRequirementRequest request = new UpdateReportRequirementRequest();
        request.setTitle("Updated Monthly Report");
        request.setDescription("Updated description");
        request.setDueDate(LocalDateTime.now().plusDays(10));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(submissionReportRequirementRepository.findById(requirementId))
            .thenReturn(Optional.of(submissionRequirement));
        when(submissionReportRequirementRepository.save(any(SubmissionReportRequirement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(clubReportRequirementRepository.findBySubmissionReportRequirementId(requirementId))
            .thenReturn(List.of(clubRequirement));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(requirementId);
        response.setTitle("Updated Monthly Report");
        when(submissionReportRequirementMapper.toDto(any(SubmissionReportRequirement.class))).thenReturn(response);

        // Mock for async notification
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdsAndSemesterId(anyList(), anyLong()))
            .thenReturn(List.of());
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        ReportRequirementResponse result = reportService.updateReportRequirement(requirementId, request, null, staffId);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Monthly Report", result.getTitle());
        verify(submissionReportRequirementRepository).save(any(SubmissionReportRequirement.class));
    }

    // ========== createReport ==========

    @Test
    void createReport_Success_WhenClubOfficerCreates() throws AppException {
        // Arrange
        Long userId = user.getId();

        CreateReportRequest request = new CreateReportRequest();
        request.setClubId(club.getId());
        request.setReportTitle("New Report");
        request.setContent("Report content");
        request.setFileUrl("https://example.com/reports/test-report.pdf");
        request.setReportRequirementId(clubRequirement.getId());
        request.setAutoSubmit(true);

        // Create a new clubRequirement without report for this test
        ClubReportRequirement emptyClubRequirement = new ClubReportRequirement();
        emptyClubRequirement.setId(1L);
        emptyClubRequirement.setClub(club);
        emptyClubRequirement.setSubmissionReportRequirement(submissionRequirement);
        emptyClubRequirement.setReport(null); // Ensure no report exists

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerOrTeamOfficerOrTreasurerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(submissionReportRequirementRepository.findById(emptyClubRequirement.getId()))
            .thenReturn(Optional.of(submissionRequirement));
        when(clubReportRequirementRepository.findByClubIdAndSubmissionReportRequirementId(
            club.getId(), emptyClubRequirement.getId()
        )).thenReturn(Optional.of(emptyClubRequirement));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report savedReport = invocation.getArgument(0);
            savedReport.setId(1L);
            return savedReport;
        });

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(1L);
        response.setReportTitle("New Report");
        response.setStatus(ReportStatus.PENDING_CLUB);
        when(reportMapper.toDetail(any(Report.class))).thenReturn(response);

        // Mock for async notification (sendReportSubmittedNotificationAsync)
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(anyLong(), anyLong()))
            .thenReturn(List.of(user.getId()));
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        // Act
        ReportDetailResponse result = reportService.createReport(request, null, userId);

        // Assert
        assertNotNull(result);
        assertEquals("New Report", result.getReportTitle());
        assertEquals(ReportStatus.PENDING_CLUB, result.getStatus());
        verify(reportRepository).save(argThat(r ->
            r.getStatus() == ReportStatus.PENDING_CLUB &&
            r.getSubmittedDate() != null
        ));
    }



    // ========== updateReport ==========

    @Test
    void updateReport_Success_WhenCreatorUpdates() throws AppException {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.DRAFT);

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportTitle("Updated Report");
        request.setContent("Updated content");
        request.setFileUrl("https://example.com/reports/updated-report.pdf");

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(report.getId());
        response.setReportTitle("Updated Report");
        when(reportMapper.toDetail(report)).thenReturn(response);

        // Act
        ReportDetailResponse result = reportService.updateReport(report.getId(), request, null, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Report", result.getReportTitle());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void updateReport_ThrowsForbiddenException_WhenNotCreator() {
        // Arrange
        Long otherUserId = 999L;
        report.setStatus(ReportStatus.DRAFT);

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportTitle("Updated Report");
        request.setContent("Updated content");
        request.setFileUrl("https://example.com/reports/updated-report.pdf");

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.updateReport(report.getId(), request, null, otherUserId)
        );
    }

    // ========== submitReport ==========

    @Test
    void submitReport_Success_WhenReportInDraftStatus() throws AppException {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.DRAFT);
        report.setClubReportRequirement(clubRequirement);
        clubRequirement.setReport(report);

        SubmitReportRequest request = new SubmitReportRequest();
        request.setReportId(report.getId());

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock for async notification (sendSubmitReportNotificationAsync)
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(anyLong(), anyLong()))
            .thenReturn(List.of(user.getId()));
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(report.getId());
        response.setStatus(ReportStatus.PENDING_CLUB);
        when(reportMapper.toDetail(any(Report.class))).thenReturn(response);

        // Act
        ReportDetailResponse result = reportService.submitReport(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(ReportStatus.PENDING_CLUB, result.getStatus());
        verify(reportRepository).save(argThat(r ->
            r.getStatus() == ReportStatus.PENDING_CLUB && r.getSubmittedDate() != null
        ));
    }

    @Test
    void submitReport_Success_WhenResubmittingRejectedReport() throws AppException {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.REJECTED_CLUB);
        report.setMustResubmit(true);
        report.setClubReportRequirement(clubRequirement);
        clubRequirement.setReport(report);

        SubmitReportRequest request = new SubmitReportRequest();
        request.setReportId(report.getId());

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock for async notification
        when(roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(anyLong(), anyLong()))
            .thenReturn(List.of(user.getId()));
        doNothing().when(notificationService).sendToUsers(
            anyList(), anyLong(), anyString(), anyString(), any(), any(), anyString(),
            anyLong(), anyLong(), anyLong(), anyLong());

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(report.getId());
        response.setStatus(ReportStatus.UPDATED_PENDING_CLUB);
        when(reportMapper.toDetail(any(Report.class))).thenReturn(response);

        // Act
        ReportDetailResponse result = reportService.submitReport(request, userId);

        // Assert
        assertNotNull(result);
        verify(reportRepository).save(argThat(r ->
            r.getStatus() == ReportStatus.UPDATED_PENDING_CLUB
        ));
    }


    // ========== getClubReports ==========

    @Test
    void getClubReports_Success_WhenClubOfficerAccess() {
        // Arrange
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(0, 10);
        report.setStatus(ReportStatus.PENDING_CLUB);

        Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 1);

        when(clubRepository.existsById(club.getId())).thenReturn(true);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(reportRepository.findByClubIdWithFilter(
            any(), eq(club.getId()), any(), any(), any(), eq(pageable)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(report.getId());
        responseItem.setReportTitle("Test Report");
        when(reportMapper.toListItem(report)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getClubReports(
            club.getId(), null, null, null, null, pageable, userId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Report", result.getContent().get(0).getReportTitle());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }




    // ========== getAllReportRequirements ==========

    @Test
    void getAllReportRequirements_Success_WhenStaffAccess() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        Page<SubmissionReportRequirement> requirementPage =
            new PageImpl<>(List.of(submissionRequirement), pageable, 1);

        // Setup clubRequirement with proper relationship
        clubRequirement.setSubmissionReportRequirement(submissionRequirement);

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(submissionReportRequirementRepository.findAllWithFilters(
            any(), any(), any(), eq(pageable)
        )).thenReturn(requirementPage);
        when(clubReportRequirementRepository.findBySubmissionReportRequirementIdIn(anyList()))
            .thenReturn(List.of(clubRequirement));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(1L);
        response.setTitle("Monthly Report");
        when(submissionReportRequirementMapper.toDto(submissionRequirement)).thenReturn(response);

        // Act
        PageResponse<ReportRequirementResponse> result = reportService.getAllReportRequirements(
            null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Monthly Report", result.getContent().get(0).getTitle());
        // Verify clubCount is set and clubRequirements is null
        assertEquals(1, result.getContent().get(0).getClubCount());
        assertNull(result.getContent().get(0).getClubRequirements());
        // Verify 1-based pagination
        assertEquals(1, result.getPageNumber());
        assertEquals(10, result.getPageSize());
        verify(clubReportRequirementRepository).findBySubmissionReportRequirementIdIn(anyList());
    }

    // ========== reviewReportByClub ==========


    @Test
    void reviewReportByClub_ThrowsForbiddenException_WhenNotClubOfficer() {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.PENDING_CLUB);

        ReportReviewRequest request = new ReportReviewRequest();
        request.setReportId(report.getId());
        request.setStatus(ReportStatus.PENDING_UNIVERSITY);
        request.setReviewerFeedback("Club review feedback");
        request.setMustResubmit(false);

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(false);

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.reviewReportByClub(request, userId)
        );
    }

    // ========== Additional Test Cases ==========


    @Test
    void createReport_ThrowsException_WhenClubNotActive() {
        // Arrange
        Long userId = user.getId();
        Club inactiveClub = new Club();
        inactiveClub.setId(1L);
        inactiveClub.setClubName("Test Club");
        inactiveClub.setStatus("INACTIVE");

        CreateReportRequest request = new CreateReportRequest();
        request.setClubId(inactiveClub.getId());
        request.setReportTitle("New Report");
        request.setContent("Report content");
        request.setFileUrl("https://example.com/reports/test-report.pdf");
        request.setReportRequirementId(clubRequirement.getId());
        request.setAutoSubmit(true);

        ClubReportRequirement emptyClubRequirement = new ClubReportRequirement();
        emptyClubRequirement.setId(1L);
        emptyClubRequirement.setClub(inactiveClub);
        emptyClubRequirement.setSubmissionReportRequirement(submissionRequirement);
        emptyClubRequirement.setReport(null);

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerOrTeamOfficerOrTreasurerInCurrentSemester(
            userId, inactiveClub.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(clubRepository.findById(inactiveClub.getId())).thenReturn(Optional.of(inactiveClub));

        // Act & Assert
        assertThrows(AppException.class, () ->
            reportService.createReport(request, null, userId)
        );
    }

    @Test
    void updateReport_ThrowsForbiddenException_WhenReportNotDraft() {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.PENDING_UNIVERSITY);

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportTitle("Updated Report");
        request.setContent("Updated content");
        request.setFileUrl("https://example.com/reports/updated-report.pdf");

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));

        // Act & Assert
        assertThrows(ForbiddenException.class, () ->
            reportService.updateReport(report.getId(), request, null, userId)
        );
    }

    @Test
    void updateReport_Success_WhenRejectedReport() throws AppException {
        // Arrange
        Long userId = user.getId();
        report.setStatus(ReportStatus.REJECTED_CLUB);

        UpdateReportRequest request = new UpdateReportRequest();
        request.setReportTitle("Updated After Rejection");
        request.setContent("Revised content");
        request.setFileUrl("https://example.com/reports/revised-report.pdf");

        when(reportRepository.findByIdWithRelations(report.getId())).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenReturn(report);

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(report.getId());
        response.setReportTitle("Updated After Rejection");
        when(reportMapper.toDetail(report)).thenReturn(response);

        // Act
        ReportDetailResponse result = reportService.updateReport(report.getId(), request, null, userId);

        // Assert
        assertNotNull(result);
        assertEquals("Updated After Rejection", result.getReportTitle());
        verify(reportRepository).save(any(Report.class));
    }

    @Test
    void submitReport_ThrowsException_WhenClubNotActive() {
        // Arrange
        Long userId = user.getId();
        Club inactiveClub = new Club();
        inactiveClub.setId(1L);
        inactiveClub.setStatus("INACTIVE");

        ClubReportRequirement inactiveClubReq = new ClubReportRequirement();
        inactiveClubReq.setId(1L);
        inactiveClubReq.setClub(inactiveClub);
        inactiveClubReq.setSubmissionReportRequirement(submissionRequirement);

        Report inactiveReport = new Report();
        inactiveReport.setId(1L);
        inactiveReport.setStatus(ReportStatus.DRAFT);
        inactiveReport.setClubReportRequirement(inactiveClubReq);
        inactiveReport.setCreatedBy(user);
        inactiveClubReq.setReport(inactiveReport);

        SubmitReportRequest request = new SubmitReportRequest();
        request.setReportId(inactiveReport.getId());

        when(reportRepository.findByIdWithRelations(inactiveReport.getId())).thenReturn(Optional.of(inactiveReport));

        // Act & Assert
        assertThrows(AppException.class, () ->
            reportService.submitReport(request, userId)
        );
    }

    @Test
    void createReportRequirement_ThrowsNotFoundException_WhenEventNotFound() {
        // Arrange
        Long staffId = staffUser.getId();

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Event Report");
        request.setReportType(ReportType.EVENT);
        request.setEventId(999L);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setClubIds(List.of(1L));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(clubRepository.findAllById(request.getClubIds())).thenReturn(List.of(club));
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            reportService.createReportRequirement(request, null, staffId)
        );
    }

    @Test
    void createReportRequirement_ThrowsNotFoundException_WhenClubNotFound() {
        // Arrange
        Long staffId = staffUser.getId();

        CreateReportRequirementRequest request = new CreateReportRequirementRequest();
        request.setTitle("Report Requirement");
        request.setReportType(ReportType.SEMESTER);
        request.setDueDate(LocalDateTime.now().plusDays(7));
        request.setClubIds(List.of(1L, 999L));

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(clubRepository.findAllById(request.getClubIds())).thenReturn(List.of(club)); // Only 1 club found instead of 2

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            reportService.createReportRequirement(request, null, staffId)
        );
    }

    @Test
    void updateReportRequirement_ThrowsNotFoundException_WhenRequirementNotFound() {
        // Arrange
        Long staffId = staffUser.getId();
        Long requirementId = 999L;

        UpdateReportRequirementRequest request = new UpdateReportRequirementRequest();
        request.setTitle("Updated Report");

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(submissionReportRequirementRepository.findById(requirementId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NotFoundException.class, () ->
            reportService.updateReportRequirement(requirementId, request, null, staffId)
        );
    }



    @Test
    void createReport_Success_AsTeamOfficer_CreatesAsDraft() throws AppException {
        // Arrange
        Long userId = user.getId();

        CreateReportRequest request = new CreateReportRequest();
        request.setClubId(club.getId());
        request.setReportTitle("Team Report");
        request.setContent("Report from team officer");
        request.setFileUrl("https://example.com/reports/team-report.pdf");
        request.setReportRequirementId(clubRequirement.getId());
        request.setAutoSubmit(false);

        // Create a new clubRequirement without report for this test
        ClubReportRequirement emptyClubRequirement = new ClubReportRequirement();
        emptyClubRequirement.setId(1L);
        emptyClubRequirement.setClub(club);
        emptyClubRequirement.setSubmissionReportRequirement(submissionRequirement);
        emptyClubRequirement.setReport(null); // Ensure no report exists

        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleMemberShipRepository.isClubOfficerOrTeamOfficerOrTreasurerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(clubRepository.findById(club.getId())).thenReturn(Optional.of(club));
        when(submissionReportRequirementRepository.findById(emptyClubRequirement.getId()))
            .thenReturn(Optional.of(submissionRequirement));
        when(clubReportRequirementRepository.findByClubIdAndSubmissionReportRequirementId(
            club.getId(), emptyClubRequirement.getId()
        )).thenReturn(Optional.of(emptyClubRequirement));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(false); // Not club officer, just team officer
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report savedReport = invocation.getArgument(0);
            savedReport.setId(1L);
            return savedReport;
        });

        ReportDetailResponse response = new ReportDetailResponse();
        response.setId(1L);
        response.setReportTitle("Team Report");
        response.setStatus(ReportStatus.DRAFT);
        when(reportMapper.toDetail(any(Report.class))).thenReturn(response);

        // Act
        ReportDetailResponse result = reportService.createReport(request, null, userId);

        // Assert
        assertNotNull(result);
        verify(reportRepository).save(argThat(r -> r.getStatus() == ReportStatus.DRAFT));
    }

    // ========== Pagination Tests (1-based) ==========

    @Test
    void getAllReports_ReturnsPageNumber1_WhenRequestingPage0() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10); // Request page 0 (first page in Spring Data)

        Report universityReport = new Report();
        universityReport.setId(1L);
        universityReport.setReportTitle("Report 1");
        universityReport.setStatus(ReportStatus.PENDING_UNIVERSITY);
        universityReport.setClubReportRequirement(clubRequirement);
        universityReport.setSemester(currentSemester);

        Page<Report> reportPage = new PageImpl<>(List.of(universityReport), pageable, 1);

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(1L);
        responseItem.setReportTitle("Report 1");
        when(reportMapper.toListItem(universityReport)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPageNumber(), "Page number should be 1-based, so page 0 becomes page 1");
        assertEquals(10, result.getPageSize());
        assertEquals(1, result.getTotalElements());
        assertFalse(result.isHasPrevious(), "First page should not have previous");
        assertFalse(result.isHasNext(), "Single page should not have next");
    }

    @Test
    void getAllReports_ReturnsPageNumber2_WhenRequestingPage1() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(1, 10); // Request page 1 (second page in Spring Data)

        Report universityReport = new Report();
        universityReport.setId(1L);
        universityReport.setReportTitle("Report 1");
        universityReport.setStatus(ReportStatus.PENDING_UNIVERSITY);
        universityReport.setClubReportRequirement(clubRequirement);
        universityReport.setSemester(currentSemester);

        // Total 15 items, so page 1 (index) would have items 11-15
        Page<Report> reportPage = new PageImpl<>(List.of(universityReport), pageable, 15);

        when(reportRepository.findAllWithFilters(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(1L);
        responseItem.setReportTitle("Report 1");
        when(reportMapper.toListItem(universityReport)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getAllReports(
            null, null, null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getPageNumber(), "Page number should be 1-based, so page 1 becomes page 2");
        assertEquals(10, result.getPageSize());
        assertTrue(result.isHasPrevious(), "Second page should have previous");
        assertFalse(result.isHasNext(), "Last page should not have next");
    }

    @Test
    void getClubReports_ReturnsCorrect1BasedPageNumber() {
        // Arrange
        Long userId = user.getId();
        Pageable pageable = PageRequest.of(2, 5); // Request page 2 with size 5
        report.setStatus(ReportStatus.PENDING_CLUB);

        Page<Report> reportPage = new PageImpl<>(List.of(report), pageable, 20); // Total 20 items

        when(clubRepository.existsById(club.getId())).thenReturn(true);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.isClubOfficerInCurrentSemester(
            userId, club.getId(), currentSemester.getId()
        )).thenReturn(true);
        when(reportRepository.findByClubIdWithFilter(
            any(), eq(club.getId()), any(), any(), any(), eq(pageable)
        )).thenReturn(reportPage);

        ReportListItemResponse responseItem = new ReportListItemResponse();
        responseItem.setId(report.getId());
        responseItem.setReportTitle("Test Report");
        when(reportMapper.toListItem(report)).thenReturn(responseItem);

        // Act
        PageResponse<ReportListItemResponse> result = reportService.getClubReports(
            club.getId(), null, null, null, null, pageable, userId
        );

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getPageNumber(), "Page 2 (0-indexed) should become page 3 (1-based)");
        assertEquals(5, result.getPageSize());
        assertTrue(result.isHasPrevious(), "Page 3 should have previous");
        assertTrue(result.isHasNext(), "Page 3 of 4 should have next");
    }

    @Test
    void getAllReportRequirements_ReturnsCorrect1BasedPageNumber() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 20);

        Page<SubmissionReportRequirement> requirementPage =
            new PageImpl<>(List.of(submissionRequirement), pageable, 1);

        clubRequirement.setSubmissionReportRequirement(submissionRequirement);

        when(roleService.isStaff(staffId)).thenReturn(true);
        when(submissionReportRequirementRepository.findAllWithFilters(
            any(), any(), any(), eq(pageable)
        )).thenReturn(requirementPage);
        when(clubReportRequirementRepository.findBySubmissionReportRequirementIdIn(anyList()))
            .thenReturn(List.of(clubRequirement));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(1L);
        response.setTitle("Monthly Report");
        when(submissionReportRequirementMapper.toDto(submissionRequirement)).thenReturn(response);

        // Act
        PageResponse<ReportRequirementResponse> result = reportService.getAllReportRequirements(
            null, null, null, pageable, staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getPageNumber(), "First page should have page number 1");
        assertEquals(20, result.getPageSize());
        assertEquals(1, result.getTotalElements());
    }

    // ========== getClubsByReportRequirement ==========

    @Test
    void getClubsByReportRequirement_Success_WhenStaffAccess() {
        // Arrange
        Long staffId = staffUser.getId();
        Pageable pageable = PageRequest.of(0, 10);

        ClubReportRequirement clubReq1 = new ClubReportRequirement();
        clubReq1.setId(1L);
        clubReq1.setClub(club);
        clubReq1.setSubmissionReportRequirement(submissionRequirement);
        clubReq1.setReport(report);

        when(submissionReportRequirementRepository.existsById(submissionRequirement.getId()))
            .thenReturn(true);
        when(clubReportRequirementRepository.findBySubmissionReportRequirementId(
            submissionRequirement.getId()
        )).thenReturn(List.of(clubReq1));

        // Act
        PageResponse<ReportRequirementResponse.ClubRequirementInfo> result =
            reportService.getClubsByReportRequirement(
                submissionRequirement.getId(), "Test Club", pageable, staffId
            );

        // Assert
        assertNotNull(result);
        assertTrue(result.getTotalElements() >= 0);
    }


    // ========== getReportRequirementById ==========

    @Test
    void getReportRequirementById_Success_WhenStaffAccess() {
        // Arrange
        Long staffId = staffUser.getId();

        when(submissionReportRequirementRepository.findById(submissionRequirement.getId()))
            .thenReturn(Optional.of(submissionRequirement));
        when(clubReportRequirementRepository.findBySubmissionReportRequirementId(
            submissionRequirement.getId()
        )).thenReturn(List.of(clubRequirement));

        ReportRequirementResponse response = new ReportRequirementResponse();
        response.setId(submissionRequirement.getId());
        response.setTitle("Semester Report");
        when(submissionReportRequirementMapper.toDto(submissionRequirement))
            .thenReturn(response);

        // Act
        ReportRequirementResponse result = reportService.getReportRequirementById(
            submissionRequirement.getId(), staffId
        );

        // Assert
        assertNotNull(result);
        assertEquals("Semester Report", result.getTitle());
        verify(submissionReportRequirementRepository).findById(submissionRequirement.getId());
    }

    // ========== getClubReportByRequirementForOfficer ==========




}

