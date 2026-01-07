package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.*;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.*;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.*;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.websocket.WebSocketService;
import com.sep490.backendclubmanagement.service.workflow.WorkflowHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test cho RequestEstablishmentService (JUnit5 + Mockito)
 * 120+ test cases covering:
 * - Happy paths for all operations
 * - Edge cases and error scenarios
 * - Validation rules
 * - Permission checks
 * - Status transitions
 * - All methods in RequestEstablishmentService
 */
@ExtendWith(MockitoExtension.class)
class RequestEstablishmentServiceTest {

    @Mock
    private RequestEstablishmentRepository requestEstablishmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClubCreationWorkFlowHistoryRepository workflowHistoryRepository;

    @Mock
    private WorkflowHistoryService workflowHistoryService;

    @Mock
    private ClubProposalRepository clubProposalRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private DefenseScheduleRepository defenseScheduleRepository;

    @Mock
    private ClubCreationFinalFormRepository clubCreationFinalFormRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubRoleRepository clubRoleRepository;

    @Mock
    private SystemRoleRepository systemRoleRepository;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private RoleMemberShipRepository roleMemberShipRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ClubCategoryRepository clubCategoryRepository;

    @Mock
    private ClubCreationStepRepository clubCreationStepRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private RequestEstablishmentService requestEstablishmentService;

    // ========= Helper Methods =========

    private User buildStudent(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Student " + id);
        user.setStudentCode("SE" + id);
        SystemRole role = new SystemRole();
        role.setId(1L);
        role.setRoleName("STUDENT");
        user.setSystemRole(role);
        return user;
    }

    private User buildStaff(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Staff " + id);
        SystemRole role = new SystemRole();
        role.setId(2L);
        role.setRoleName("STAFF");
        user.setSystemRole(role);
        return user;
    }

    private RequestEstablishment buildRequest(Long id, Long createdById, RequestEstablishmentStatus status) {
        User creator = buildStudent(createdById, "student" + createdById + "@fpt.edu.vn");
        RequestEstablishment request = RequestEstablishment.builder()
                .id(id)
                .clubName("Test Club " + id)
                .clubCategory("Category")
                .clubCode("CLB" + id)
                .status(status)
                .expectedMemberCount(50)
                .description("Description")
                .email("club" + id + "@fpt.edu.vn")
                .phone("0123456789")
                .createdBy(creator)
                .build();
        return request;
    }

    private RequestEstablishment buildRequestWithStaff(Long id, Long createdById, Long staffId, RequestEstablishmentStatus status) {
        RequestEstablishment request = buildRequest(id, createdById, status);
        User staff = buildStaff(staffId, "staff" + staffId + "@fpt.edu.vn");
        request.setAssignedStaff(staff);
        return request;
    }

    private CreateRequestEstablishmentRequest buildCreateRequest(boolean isDraft) {
        CreateRequestEstablishmentRequest request = new CreateRequestEstablishmentRequest();
        request.setClubName("New Club");
        request.setClubCategory("Category");
        request.setExpectedMemberCount(50);
        request.setEmail("newclub@fpt.edu.vn");
        request.setPhone("0123456789");
        request.setIsDraft(isDraft);
        return request;
    }

    private ClubProposal buildProposal(Long id, Long requestId) {
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal = ClubProposal.builder()
                .id(id)
                .title("Proposal " + id)
                .fileUrl("https://file.com/proposal" + id + ".pdf")
                .requestEstablishment(request)
                .build();
        return proposal;
    }

    private DefenseSchedule buildDefenseSchedule(Long id, Long requestId) {
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = DefenseSchedule.builder()
                .id(id)
                .defenseDate(LocalDateTime.now().plusDays(7))
                .defenseEndDate(LocalDateTime.now().plusDays(7).plusHours(2))
                .location("Room 101")
                .meetingLink("https://meet.com/defense")
                .requestEstablishment(request)
                .result(DefenseScheduleStatus.PROPOSED)
                .build();
        return schedule;
    }

    private ClubCreationFinalForm buildFinalForm(Long id, Long requestId) {
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        User student = buildStudent(1L, "student1@fpt.edu.vn");
        ClubCreationFinalForm form = ClubCreationFinalForm.builder()
                .id(id)
                .formData("{\"title\":\"Final Form\",\"fileUrl\":\"https://file.com/final.pdf\"}")
                .status("SUBMITTED")
                .requestEstablishment(request)
                .submittedBy(student)
                .submittedAt(LocalDateTime.now())
                .build();
        return form;
    }

    private Semester buildSemester(Long id, boolean isCurrent) {
        Semester semester = new Semester();
        semester.setId(id);
        semester.setSemesterName("Spring 2025");
        semester.setIsCurrent(isCurrent);
        semester.setStartDate(LocalDate.of(2025, 1, 1));
        semester.setEndDate(LocalDate.of(2025, 6, 30));
        return semester;
    }

    // ==========================================
    // STUDENT OPERATIONS - createRequest (8 test cases)
    // ==========================================

    @Test
    void createRequest_whenIsDraftTrue_shouldCreateDraftRequest() throws AppException {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);
        User student = buildStudent(userId, "student1@fpt.edu.vn");

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(clubRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenAnswer(invocation -> {
            RequestEstablishment req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.createRequest(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("New Club", response.getClubName());
        assertEquals(RequestEstablishmentStatus.DRAFT, response.getStatus());
        assertNull(response.getSendDate());
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void createRequest_whenIsDraftFalse_shouldCreateSubmittedRequest() throws AppException {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(false);
        User student = buildStudent(userId, "student1@fpt.edu.vn");

        when(userRepository.findById(userId)).thenReturn(Optional.of(student));
        when(clubRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenAnswer(invocation -> {
            RequestEstablishment req = invocation.getArgument(0);
            req.setId(100L);
            req.setSendDate(LocalDateTime.now());
            return req;
        });

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.createRequest(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.SUBMITTED, response.getStatus());
        assertNotNull(response.getSendDate());
    }

    @Test
    void createRequest_whenClubNameIsEmpty_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);
        request.setClubName("");

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Tên CLB không được để trống"));
        verify(requestEstablishmentRepository, never()).save(any());
    }

    @Test
    void createRequest_whenClubNameExists_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);

        when(clubRepository.existsByClubNameIgnoreCase("New Club")).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Tên CLB này đã tồn tại"));
    }

    @Test
    void createRequest_whenUserNotFound_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);

        when(clubRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void createRequest_whenClubCategoryIsEmpty_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);
        request.setClubCategory("");

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Danh mục CLB không được để trống"));
    }

    @Test
    void createRequest_whenExpectedMemberCountIsZero_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);
        request.setExpectedMemberCount(0);

        // Note: clubRepository.existsByClubNameIgnoreCase and requestEstablishmentRepository.existsByClubNameIgnoreCase
        // are not called because validation throws exception before reaching validateClubNameUniqueness()

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Số lượng thành viên dự kiến phải lớn hơn 0"));
    }

    @Test
    void createRequest_whenClubCodeExists_shouldThrowException() {
        // Arrange
        Long userId = 1L;
        CreateRequestEstablishmentRequest request = buildCreateRequest(true);
        request.setClubCode("EXISTING_CODE");

        when(clubRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(clubRepository.existsByClubCodeIgnoreCase("EXISTING_CODE")).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.createRequest(userId, request));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Mã CLB này đã tồn tại"));
    }

    // ==========================================
    // STUDENT OPERATIONS - getMyRequests (2 test cases)
    // ==========================================

    @Test
    void getMyRequests_whenStatusIsNull_shouldReturnAllRequests() throws AppException {
        // Arrange
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        RequestEstablishment request1 = buildRequest(100L, userId, RequestEstablishmentStatus.DRAFT);
        RequestEstablishment request2 = buildRequest(101L, userId, RequestEstablishmentStatus.SUBMITTED);
        Page<RequestEstablishment> page = new PageImpl<>(List.of(request1, request2));

        when(requestEstablishmentRepository.findByCreatedBy(userId, pageable)).thenReturn(page);

        // Act
        Page<RequestEstablishmentResponse> response = requestEstablishmentService.getMyRequests(userId, null, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
        verify(requestEstablishmentRepository).findByCreatedBy(userId, pageable);
    }

    @Test
    void getMyRequests_whenStatusIsProvided_shouldReturnFilteredRequests() throws AppException {
        // Arrange
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        RequestEstablishment request = buildRequest(100L, userId, RequestEstablishmentStatus.DRAFT);
        Page<RequestEstablishment> page = new PageImpl<>(List.of(request));

        when(requestEstablishmentRepository.findByCreatedByAndStatus(userId, RequestEstablishmentStatus.DRAFT, pageable))
                .thenReturn(page);

        // Act
        Page<RequestEstablishmentResponse> response = requestEstablishmentService.getMyRequests(userId, RequestEstablishmentStatus.DRAFT, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(RequestEstablishmentStatus.DRAFT, response.getContent().get(0).getStatus());
    }

    // ==========================================
    // STUDENT OPERATIONS - getRequestDetail (3 test cases)
    // ==========================================

    @Test
    void getRequestDetail_whenOwner_shouldReturnRequest() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.getRequestDetail(requestId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(requestId, response.getId());
        assertEquals(userId, response.getCreatedByUserId());
    }

    @Test
    void getRequestDetail_whenNotOwner_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;
        RequestEstablishment request = buildRequest(requestId, otherUserId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getRequestDetail(requestId, userId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("không có quyền"));
    }

    @Test
    void getRequestDetail_whenRequestNotFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getRequestDetail(requestId, userId));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // STUDENT OPERATIONS - updateRequest (3 test cases)
    // ==========================================

    @Test
    void updateRequest_whenDraft_shouldUpdateSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        UpdateRequestEstablishmentRequest updateRequest = new UpdateRequestEstablishmentRequest();
        updateRequest.setClubName("Updated Club Name");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.updateRequest(requestId, userId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void updateRequest_whenNotDraft_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.SUBMITTED);
        UpdateRequestEstablishmentRequest updateRequest = new UpdateRequestEstablishmentRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.updateRequest(requestId, userId, updateRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    void updateRequest_whenNotOwner_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;
        RequestEstablishment request = buildRequest(requestId, otherUserId, RequestEstablishmentStatus.DRAFT);
        UpdateRequestEstablishmentRequest updateRequest = new UpdateRequestEstablishmentRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.updateRequest(requestId, userId, updateRequest));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ==========================================
    // STUDENT OPERATIONS - deleteRequest (2 test cases)
    // ==========================================

    @Test
    void deleteRequest_whenDraft_shouldDeleteSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        doNothing().when(requestEstablishmentRepository).delete(request);

        // Act
        requestEstablishmentService.deleteRequest(requestId, userId);

        // Assert
        verify(requestEstablishmentRepository).delete(request);
    }

    @Test
    void deleteRequest_whenNotDraft_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.deleteRequest(requestId, userId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        verify(requestEstablishmentRepository, never()).delete(any());
    }

    // ==========================================
    // STUDENT OPERATIONS - submitRequest (5 test cases)
    // ==========================================

    @Test
    void submitRequest_whenDraft_shouldChangeToSubmitted() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        when(userRepository.findBySystemRole_RoleNameIgnoreCase("STAFF")).thenReturn(List.of());
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.submitRequest(requestId, userId);

        // Assert
        assertNotNull(response);
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void submitRequest_whenNotDraft_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitRequest(requestId, userId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("DRAFT"));
    }

    @Test
    void submitRequest_whenClubNameIsEmpty_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        request.setClubName("");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitRequest(requestId, userId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Tên CLB không được để trống"));
    }

    @Test
    void submitRequest_whenClubCategoryIsEmpty_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        request.setClubCategory("");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitRequest(requestId, userId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Danh mục CLB không được để trống"));
    }

    @Test
    void submitRequest_whenExpectedMemberCountIsZero_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        request.setExpectedMemberCount(0);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitRequest(requestId, userId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Số lượng thành viên dự kiến phải lớn hơn 0"));
    }

    // ==========================================
    // STUDENT OPERATIONS - submitProposal (6 test cases)
    // ==========================================

    @Test
    void submitProposal_whenValidFile_shouldSubmitSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_REQUIRED);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();
        proposalRequest.setTitle("Proposal Title");
        proposalRequest.setFileUrl(null);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("proposal.pdf");
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn(
                new CloudinaryService.UploadResult("https://file.com/proposal.pdf", "public-id", "pdf", 1000L));
        when(clubProposalRepository.save(any(ClubProposal.class))).thenAnswer(invocation -> {
            ClubProposal p = invocation.getArgument(0);
            p.setId(200L);
            return p;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, multipartFile);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_SUBMITTED, response.getStatus());
        verify(clubProposalRepository).save(any(ClubProposal.class));
    }

    @Test
    void submitProposal_whenNoFileAndNoFileUrl_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.PROPOSAL_REQUIRED);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();
        proposalRequest.setTitle("Proposal Title");
        proposalRequest.setFileUrl(null);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, null));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("file đề án"));
    }

    @Test
    void submitProposal_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, multipartFile));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void submitProposal_whenProposalRejected_shouldAllowResubmit() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_REJECTED);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();
        proposalRequest.setTitle("Resubmitted Proposal");
        proposalRequest.setFileUrl("https://file.com/resubmit.pdf");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.save(any(ClubProposal.class))).thenAnswer(invocation -> {
            ClubProposal p = invocation.getArgument(0);
            p.setId(200L);
            return p;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, null);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_SUBMITTED, response.getStatus());
    }

    @Test
    void submitProposal_whenProposalSubmitted_shouldAllowUpdate() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();
        proposalRequest.setTitle("Updated Proposal");
        proposalRequest.setFileUrl("https://file.com/update.pdf");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.save(any(ClubProposal.class))).thenAnswer(invocation -> {
            ClubProposal p = invocation.getArgument(0);
            p.setId(201L);
            return p;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, null);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_SUBMITTED, response.getStatus());
    }

    @Test
    void submitProposal_whenInvalidFileType_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_REQUIRED);
        SubmitProposalRequest proposalRequest = new SubmitProposalRequest();
        proposalRequest.setTitle("Proposal Title");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("proposal.exe");

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitProposal(requestId, userId, proposalRequest, multipartFile));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chỉ chấp nhận file"));
    }

    // ==========================================
    // STUDENT OPERATIONS - proposeDefenseSchedule (4 test cases)
    // ==========================================

    @Test
    void proposeDefenseSchedule_whenValid_shouldProposeSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_APPROVED);
        ProposeDefenseScheduleRequest scheduleRequest = new ProposeDefenseScheduleRequest();
        scheduleRequest.setDefenseDate(LocalDateTime.now().plusDays(7));
        scheduleRequest.setDefenseEndDate(LocalDateTime.now().plusDays(7).plusHours(2));
        scheduleRequest.setLocation("Room 101");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.empty());
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenAnswer(invocation -> {
            DefenseSchedule s = invocation.getArgument(0);
            s.setId(300L);
            return s;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        DefenseScheduleResponse response = requestEstablishmentService.proposeDefenseSchedule(requestId, userId, scheduleRequest);

        // Assert
        assertNotNull(response);
        assertEquals(300L, response.getId());
        verify(defenseScheduleRepository).save(any(DefenseSchedule.class));
    }

    @Test
    void proposeDefenseSchedule_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        ProposeDefenseScheduleRequest scheduleRequest = new ProposeDefenseScheduleRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.proposeDefenseSchedule(requestId, userId, scheduleRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void proposeDefenseSchedule_whenDefenseScheduleRejected_shouldAllowResubmit() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.DEFENSE_SCHEDULE_REJECTED);
        ProposeDefenseScheduleRequest scheduleRequest = new ProposeDefenseScheduleRequest();
        scheduleRequest.setDefenseDate(LocalDateTime.now().plusDays(7));
        scheduleRequest.setDefenseEndDate(LocalDateTime.now().plusDays(7).plusHours(2));
        scheduleRequest.setLocation("Room 102");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        DefenseSchedule existingSchedule = buildDefenseSchedule(300L, requestId);
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(existingSchedule));
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenReturn(existingSchedule);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        DefenseScheduleResponse response = requestEstablishmentService.proposeDefenseSchedule(requestId, userId, scheduleRequest);

        // Assert
        assertNotNull(response);
        verify(defenseScheduleRepository).save(any(DefenseSchedule.class));
    }

    @Test
    void proposeDefenseSchedule_whenDefenseEndDateBeforeDefenseDate_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.PROPOSAL_APPROVED);
        ProposeDefenseScheduleRequest scheduleRequest = new ProposeDefenseScheduleRequest();
        scheduleRequest.setDefenseDate(LocalDateTime.now().plusDays(7));
        scheduleRequest.setDefenseEndDate(LocalDateTime.now().plusDays(6)); // Before defense date

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.proposeDefenseSchedule(requestId, userId, scheduleRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Thời gian kết thúc bảo vệ phải sau thời gian bắt đầu"));
    }

    // ==========================================
    // STUDENT OPERATIONS - updateDefenseSchedule (2 test cases)
    // ==========================================

    @Test
    void updateDefenseSchedule_whenValid_shouldUpdateSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        ProposeDefenseScheduleRequest updateRequest = new ProposeDefenseScheduleRequest();
        updateRequest.setDefenseDate(LocalDateTime.now().plusDays(8));
        updateRequest.setDefenseEndDate(LocalDateTime.now().plusDays(8).plusHours(2));
        updateRequest.setLocation("Room 201");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenReturn(schedule);

        // Act
        DefenseScheduleResponse response = requestEstablishmentService.updateDefenseSchedule(requestId, userId, updateRequest);

        // Assert
        assertNotNull(response);
        verify(defenseScheduleRepository).save(any(DefenseSchedule.class));
    }

    @Test
    void updateDefenseSchedule_whenScheduleConfirmed_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        schedule.setResult(DefenseScheduleStatus.CONFIRMED);
        ProposeDefenseScheduleRequest updateRequest = new ProposeDefenseScheduleRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.updateDefenseSchedule(requestId, userId, updateRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("đã được xác nhận"));
    }

    // ==========================================
    // STUDENT OPERATIONS - submitFinalForm (4 test cases)
    // ==========================================

    @Test
    void submitFinalForm_whenValid_shouldSubmitSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.DEFENSE_COMPLETED);
        SubmitFinalFormRequest formRequest = new SubmitFinalFormRequest();
        formRequest.setTitle("Final Form");
        formRequest.setFileUrl(null);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("final.pdf");
        when(cloudinaryService.uploadFile(any(), anyString())).thenReturn(
                new CloudinaryService.UploadResult("https://file.com/final.pdf", "public-id", "pdf", 1000L));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildStudent(userId, "student1@fpt.edu.vn")));
        when(clubCreationFinalFormRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.empty());
        when(clubCreationFinalFormRepository.save(any(ClubCreationFinalForm.class))).thenAnswer(invocation -> {
            ClubCreationFinalForm f = invocation.getArgument(0);
            f.setId(400L);
            return f;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        ClubCreationFinalFormResponse response = requestEstablishmentService.submitFinalForm(requestId, userId, formRequest, multipartFile);

        // Assert
        assertNotNull(response);
        assertEquals(400L, response.getId());
        verify(clubCreationFinalFormRepository).save(any(ClubCreationFinalForm.class));
    }

    @Test
    void submitFinalForm_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DRAFT);
        SubmitFinalFormRequest formRequest = new SubmitFinalFormRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitFinalForm(requestId, userId, formRequest, multipartFile));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void submitFinalForm_whenFinalFormSubmitted_shouldAllowUpdate() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        SubmitFinalFormRequest formRequest = new SubmitFinalFormRequest();
        formRequest.setTitle("Updated Final Form");
        formRequest.setFileUrl("https://file.com/updated.pdf");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(userId)).thenReturn(Optional.of(buildStudent(userId, "student1@fpt.edu.vn")));
        ClubCreationFinalForm existingForm = buildFinalForm(400L, requestId);
        when(clubCreationFinalFormRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(existingForm));
        when(clubCreationFinalFormRepository.save(any(ClubCreationFinalForm.class))).thenReturn(existingForm);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        ClubCreationFinalFormResponse response = requestEstablishmentService.submitFinalForm(requestId, userId, formRequest, null);

        // Assert
        assertNotNull(response);
        verify(clubCreationFinalFormRepository).save(any(ClubCreationFinalForm.class));
    }

    @Test
    void submitFinalForm_whenInvalidFileType_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.DEFENSE_COMPLETED);
        SubmitFinalFormRequest formRequest = new SubmitFinalFormRequest();
        formRequest.setTitle("Final Form");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("final.exe");

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitFinalForm(requestId, userId, formRequest, multipartFile));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chỉ chấp nhận file"));
    }

    // ==========================================
    // STUDENT OPERATIONS - submitNameRevision (2 test cases)
    // ==========================================

    @Test
    void submitNameRevision_whenValid_shouldUpdateNameSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequestWithStaff(requestId, userId, 10L, RequestEstablishmentStatus.NAME_REVISION_REQUIRED);
        RenameClubRequest renameRequest = new RenameClubRequest();
        renameRequest.setNewClubName("Updated Club Name");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubRepository.existsByClubNameIgnoreCase(anyString())).thenReturn(false);
        when(requestEstablishmentRepository.existsByClubNameIgnoreCaseAndIdNot(anyString(), anyLong())).thenReturn(false);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenAnswer(invocation -> {
            RequestEstablishment req = invocation.getArgument(0);
            req.setClubName("Updated Club Name");
            req.setStatus(RequestEstablishmentStatus.CONTACT_CONFIRMED);
            return req;
        });
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.submitNameRevision(requestId, userId, renameRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Updated Club Name", response.getClubName());
        assertEquals(RequestEstablishmentStatus.CONTACT_CONFIRMED, response.getStatus());
    }

    @Test
    void submitNameRevision_whenDuplicateName_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.NAME_REVISION_REQUIRED);
        RenameClubRequest renameRequest = new RenameClubRequest();
        renameRequest.setNewClubName("Existing Club");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubRepository.existsByClubNameIgnoreCase("Existing Club")).thenReturn(true);

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.submitNameRevision(requestId, userId, renameRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Tên CLB này đã tồn tại"));
    }

    // ==========================================
    // STAFF OPERATIONS - getPendingRequests (1 test case)
    // ==========================================

    @Test
    void getPendingRequests_shouldReturnPendingRequests() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        RequestEstablishment request1 = buildRequest(100L, 1L, RequestEstablishmentStatus.SUBMITTED);
        RequestEstablishment request2 = buildRequest(101L, 2L, RequestEstablishmentStatus.CONTACT_CONFIRMED);
        Page<RequestEstablishment> page = new PageImpl<>(List.of(request1, request2));

        when(requestEstablishmentRepository.findByStatusIn(anyList(), eq(pageable))).thenReturn(page);

        // Act
        Page<RequestEstablishmentResponse> response = requestEstablishmentService.getPendingRequests(pageable);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

    // ==========================================
    // STAFF OPERATIONS - assignRequest (4 test cases)
    // ==========================================

    @Test
    void assignRequest_whenValid_shouldAssignSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);
        AssignRequestEstablishmentRequest assignRequest = new AssignRequestEstablishmentRequest();
        assignRequest.setStaffId(20L);
        User assignedStaff = buildStaff(20L, "staff20@fpt.edu.vn");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(20L)).thenReturn(Optional.of(assignedStaff));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.assignRequest(requestId, staffId, assignRequest);

        // Assert
        assertNotNull(response);
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void assignRequest_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.DRAFT);
        AssignRequestEstablishmentRequest assignRequest = new AssignRequestEstablishmentRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.assignRequest(requestId, staffId, assignRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("SUBMITTED"));
    }

    @Test
    void assignRequest_whenStaffIdIsNull_shouldAutoAssign() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);
        AssignRequestEstablishmentRequest assignRequest = new AssignRequestEstablishmentRequest();
        assignRequest.setStaffId(null); // Null means auto-assign to current staff

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(buildStaff(staffId, "staff10@fpt.edu.vn")));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.assignRequest(requestId, staffId, assignRequest);

        // Assert
        assertNotNull(response);
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void assignRequest_whenStaffNotFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);
        AssignRequestEstablishmentRequest assignRequest = new AssignRequestEstablishmentRequest();
        assignRequest.setStaffId(999L);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.assignRequest(requestId, staffId, assignRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - receiveRequest (3 test cases)
    // ==========================================

    @Test
    void receiveRequest_whenValid_shouldReceiveAndSetDeadline() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);
        User staff = buildStaff(staffId, "staff10@fpt.edu.vn");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.receiveRequest(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING, response.getStatus());
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void receiveRequest_whenNotAssigned_shouldAutoAssign() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);
        request.setAssignedStaff(null);
        User staff = buildStaff(staffId, "staff10@fpt.edu.vn");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.receiveRequest(requestId, staffId);

        // Assert
        assertNotNull(response);
        verify(requestEstablishmentRepository).save(any(RequestEstablishment.class));
    }

    @Test
    void receiveRequest_whenAssignedToOther_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        Long otherStaffId = 20L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, otherStaffId, RequestEstablishmentStatus.SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.receiveRequest(requestId, staffId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Nhân viên phòng IC-PDP khác"));
    }

    // ==========================================
    // STAFF OPERATIONS - confirmContact (4 test cases)
    // ==========================================

    @Test
    void confirmContact_whenValid_shouldConfirmSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING);
        request.setConfirmationDeadline(LocalDateTime.now().plusDays(5));

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.confirmContact(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.CONTACT_CONFIRMED, response.getStatus());
        assertNotNull(response.getConfirmedAt());
    }

    @Test
    void confirmContact_whenNotAssignedStaff_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        Long otherStaffId = 20L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, otherStaffId, RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.confirmContact(requestId, staffId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void confirmContact_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.confirmContact(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void confirmContact_whenDeadlineExpired_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING);
        request.setConfirmationDeadline(LocalDateTime.now().minusDays(1)); // Expired

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.confirmContact(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Đã quá hạn xác nhận"));
    }

    // ==========================================
    // STAFF OPERATIONS - rejectContact (2 test cases)
    // ==========================================

    @Test
    void rejectContact_whenValid_shouldRejectSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING);
        RejectContactRequest rejectRequest = new RejectContactRequest();
        rejectRequest.setReason("Invalid contact");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.rejectContact(requestId, staffId, rejectRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.CONTACT_REJECTED, response.getStatus());
    }

    @Test
    void rejectContact_whenNotAssignedStaff_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        Long otherStaffId = 20L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, otherStaffId, RequestEstablishmentStatus.CONTACT_CONFIRMATION_PENDING);
        RejectContactRequest rejectRequest = new RejectContactRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.rejectContact(requestId, staffId, rejectRequest));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - requestProposal (2 test cases)
    // ==========================================

    @Test
    void requestProposal_whenValid_shouldRequestSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.CONTACT_CONFIRMED);
        RequestProposalRequest proposalRequest = new RequestProposalRequest();
        proposalRequest.setComment("Please submit proposal");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.requestProposal(requestId, staffId, proposalRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_REQUIRED, response.getStatus());
    }

    @Test
    void requestProposal_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);
        RequestProposalRequest proposalRequest = new RequestProposalRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.requestProposal(requestId, staffId, proposalRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - approveProposal (3 test cases)
    // ==========================================

    @Test
    void approveProposal_whenValid_shouldApproveSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal = buildProposal(200L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(proposal));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.approveProposal(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_APPROVED, response.getStatus());
    }

    @Test
    void approveProposal_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveProposal(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void approveProposal_whenNoProposalFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveProposal(requestId, staffId));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy đề án"));
    }

    // ==========================================
    // STAFF OPERATIONS - rejectProposal (3 test cases)
    // ==========================================

    @Test
    void rejectProposal_whenValid_shouldRejectSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal = buildProposal(200L, requestId);
        RejectProposalRequest rejectRequest = new RejectProposalRequest();
        rejectRequest.setReason("Insufficient details");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(proposal));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.rejectProposal(requestId, staffId, rejectRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.PROPOSAL_REJECTED, response.getStatus());
    }

    @Test
    void rejectProposal_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);
        RejectProposalRequest rejectRequest = new RejectProposalRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.rejectProposal(requestId, staffId, rejectRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void rejectProposal_whenNoProposalFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        RejectProposalRequest rejectRequest = new RejectProposalRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.rejectProposal(requestId, staffId, rejectRequest));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy đề án"));
    }

    // ==========================================
    // STAFF OPERATIONS - approveDefenseSchedule (3 test cases)
    // ==========================================

    @Test
    void approveDefenseSchedule_whenValid_shouldApproveSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenReturn(schedule);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.approveDefenseSchedule(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.DEFENSE_SCHEDULE_APPROVED, response.getStatus());
    }

    @Test
    void approveDefenseSchedule_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveDefenseSchedule(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void approveDefenseSchedule_whenNoScheduleFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveDefenseSchedule(requestId, staffId));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy lịch bảo vệ"));
    }

    // ==========================================
    // STAFF OPERATIONS - rejectDefenseSchedule (2 test cases)
    // ==========================================

    @Test
    void rejectDefenseSchedule_whenValid_shouldRejectSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        RejectDefenseScheduleRequest rejectRequest = new RejectDefenseScheduleRequest();
        rejectRequest.setReason("Time conflict");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.rejectDefenseSchedule(requestId, staffId, rejectRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.DEFENSE_SCHEDULE_REJECTED, response.getStatus());
    }

    @Test
    void rejectDefenseSchedule_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);
        RejectDefenseScheduleRequest rejectRequest = new RejectDefenseScheduleRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.rejectDefenseSchedule(requestId, staffId, rejectRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - completeDefense (5 test cases)
    // ==========================================

    @Test
    void completeDefense_whenPassed_shouldSetStatusToDefenseCompleted() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_APPROVED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        schedule.setDefenseDate(LocalDateTime.now().minusDays(1)); // Past date
        CompleteDefenseRequest completeRequest = new CompleteDefenseRequest();
        completeRequest.setResult(DefenseScheduleStatus.PASSED);
        completeRequest.setFeedback("Good defense");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenReturn(schedule);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.completeDefense(requestId, staffId, completeRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.DEFENSE_COMPLETED, response.getStatus());
    }

    @Test
    void completeDefense_whenFailed_shouldSetStatusToRejected() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_APPROVED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        schedule.setDefenseDate(LocalDateTime.now().minusDays(1));
        CompleteDefenseRequest completeRequest = new CompleteDefenseRequest();
        completeRequest.setResult(DefenseScheduleStatus.FAILED);
        completeRequest.setFeedback("Failed defense");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));
        when(defenseScheduleRepository.save(any(DefenseSchedule.class))).thenReturn(schedule);
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.completeDefense(requestId, staffId, completeRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.REJECTED, response.getStatus());
    }

    @Test
    void completeDefense_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);
        CompleteDefenseRequest completeRequest = new CompleteDefenseRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.completeDefense(requestId, staffId, completeRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void completeDefense_whenDefenseDateNotPassed_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_APPROVED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);
        schedule.setDefenseDate(LocalDateTime.now().plusDays(1)); // Future date
        CompleteDefenseRequest completeRequest = new CompleteDefenseRequest();
        completeRequest.setResult(DefenseScheduleStatus.PASSED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.completeDefense(requestId, staffId, completeRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Chưa đến thời gian bảo vệ"));
    }

    @Test
    void completeDefense_whenInvalidResult_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_APPROVED);
        CompleteDefenseRequest completeRequest = new CompleteDefenseRequest();
        completeRequest.setResult(DefenseScheduleStatus.PROPOSED); // Invalid result

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        // Note: defenseScheduleRepository.findByRequestEstablishmentId is not called because 
        // validation throws exception before reaching that code

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.completeDefense(requestId, staffId, completeRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Kết quả bảo vệ chỉ có thể là PASSED hoặc FAILED"));
    }

    // ==========================================
    // STAFF OPERATIONS - approveFinalForm (6 test cases)
    // ==========================================

    @Test
    void approveFinalForm_whenValid_shouldApproveAndCreateClub() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        ClubCreationFinalForm finalForm = buildFinalForm(400L, requestId);
        User staff = buildStaff(staffId, "staff10@fpt.edu.vn");
        Semester currentSemester = buildSemester(1L, true);
        ClubRole presidentRole = ClubRole.builder()
                .id(500L)
                .roleCode("CLUB_PRESIDENT")
                .roleName("Chủ nhiệm")
                .build();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findFirstByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(Optional.of(finalForm));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(clubRepository.findByClubCode(anyString())).thenReturn(Optional.empty());
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club c = invocation.getArgument(0);
            c.setId(600L);
            return c;
        });
        when(clubRoleRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ClubRole> roles = invocation.getArgument(0);
            roles.forEach(r -> r.setId(500L + roles.indexOf(r)));
            return roles;
        });
        when(clubMemberShipRepository.save(any(ClubMemberShip.class))).thenAnswer(invocation -> {
            ClubMemberShip m = invocation.getArgument(0);
            m.setId(700L);
            return m;
        });
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(currentSemester));
        when(roleMemberShipRepository.save(any(RoleMemberShip.class))).thenAnswer(invocation -> {
            RoleMemberShip rm = invocation.getArgument(0);
            rm.setId(800L);
            return rm;
        });
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.approveFinalForm(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.APPROVED, response.getStatus());
        verify(clubRepository).save(any(Club.class));
        verify(clubRoleRepository).saveAll(anyList());
    }

    @Test
    void approveFinalForm_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveFinalForm(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    @Test
    void approveFinalForm_whenNotAssignedStaff_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        Long otherStaffId = 20L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, otherStaffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveFinalForm(requestId, staffId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    @Test
    void approveFinalForm_whenNoFinalFormFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findFirstByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveFinalForm(requestId, staffId));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy Hồ sơ hoàn thiện"));
    }

    @Test
    void approveFinalForm_whenClubCodeDuplicate_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        request.setClubCode("DUPLICATE_CODE");
        ClubCreationFinalForm finalForm = buildFinalForm(400L, requestId);
        User staff = buildStaff(staffId, "staff10@fpt.edu.vn");
        Club existingClub = new Club();
        existingClub.setId(999L);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findFirstByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(Optional.of(finalForm));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(clubRepository.findByClubCode("DUPLICATE_CODE")).thenReturn(Optional.of(existingClub));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveFinalForm(requestId, staffId));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Mã CLB đã tồn tại"));
    }

    @Test
    void approveFinalForm_whenNoCurrentSemester_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        ClubCreationFinalForm finalForm = buildFinalForm(400L, requestId);
        User staff = buildStaff(staffId, "staff10@fpt.edu.vn");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findFirstByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(Optional.of(finalForm));
        when(userRepository.findById(staffId)).thenReturn(Optional.of(staff));
        when(clubRepository.findByClubCode(anyString())).thenReturn(Optional.empty());
        when(clubRepository.save(any(Club.class))).thenAnswer(invocation -> {
            Club c = invocation.getArgument(0);
            c.setId(600L);
            return c;
        });
        when(clubRoleRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<ClubRole> roles = invocation.getArgument(0);
            roles.forEach(r -> r.setId(500L + roles.indexOf(r)));
            return roles;
        });
        when(clubMemberShipRepository.save(any(ClubMemberShip.class))).thenAnswer(invocation -> {
            ClubMemberShip m = invocation.getArgument(0);
            m.setId(700L);
            return m;
        });
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.approveFinalForm(requestId, staffId));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không tìm thấy học kỳ hiện tại"));
    }

    // ==========================================
    // STAFF OPERATIONS - requestNameRevision (2 test cases)
    // ==========================================

    @Test
    void requestNameRevision_whenValid_shouldRequestSuccessfully() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.CONTACT_CONFIRMED);
        RequestNameRevisionRequest revisionRequest = new RequestNameRevisionRequest();
        revisionRequest.setComment("Please update club name");

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(requestEstablishmentRepository.save(any(RequestEstablishment.class))).thenReturn(request);
        doNothing().when(workflowHistoryService).createWorkflowHistory(anyLong(), anyLong(), anyString(), anyString());

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.requestNameRevision(requestId, staffId, revisionRequest);

        // Assert
        assertNotNull(response);
        assertEquals(RequestEstablishmentStatus.NAME_REVISION_REQUIRED, response.getStatus());
    }

    @Test
    void requestNameRevision_whenInvalidStatus_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DRAFT);
        RequestNameRevisionRequest revisionRequest = new RequestNameRevisionRequest();

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.requestNameRevision(requestId, staffId, revisionRequest));

        assertEquals(ErrorCode.INVALID_INPUT, ex.getErrorCode());
    }

    // ==========================================
    // SHARED OPERATIONS - getWorkflowHistory (2 test cases)
    // ==========================================

    @Test
    void getWorkflowHistory_whenValid_shouldReturnHistory() throws AppException {
        // Arrange
        Long requestId = 100L;
        Pageable pageable = PageRequest.of(0, 10);
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.DRAFT);
        ClubCreationWorkFlowHistory history = new ClubCreationWorkFlowHistory();
        history.setId(900L);
        history.setComments("Test comment");
        Page<ClubCreationWorkFlowHistory> page = new PageImpl<>(List.of(history));

        when(requestEstablishmentRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(workflowHistoryRepository.findByRequestEstablishmentId(requestId, pageable)).thenReturn(page);

        // Act
        Page<WorkflowHistoryResponse> response = requestEstablishmentService.getWorkflowHistory(requestId, pageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void getWorkflowHistory_whenRequestNotFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;
        Pageable pageable = PageRequest.of(0, 10);

        when(requestEstablishmentRepository.findById(requestId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getWorkflowHistory(requestId, pageable));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // SHARED OPERATIONS - getAllSteps (1 test case)
    // ==========================================

    @Test
    void getAllSteps_shouldReturnActiveSteps() {
        // Arrange
        ClubCreationStep step1 = ClubCreationStep.builder()
                .id(1L)
                .code("STEP1")
                .name("Step 1")
                .orderIndex(1)
                .active(true)
                .build();
        ClubCreationStep step2 = ClubCreationStep.builder()
                .id(2L)
                .code("STEP2")
                .name("Step 2")
                .orderIndex(2)
                .active(true)
                .build();

        when(clubCreationStepRepository.findByActiveTrueOrderByOrderIndexAsc()).thenReturn(List.of(step1, step2));

        // Act
        List<ClubCreationStepResponse> response = requestEstablishmentService.getAllSteps();

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals(1, response.get(0).getOrderIndex());
        assertEquals(2, response.get(1).getOrderIndex());
    }

    // ==========================================
    // SHARED OPERATIONS - getProposals (student) (1 test case)
    // ==========================================

    @Test
    void getProposals_whenValid_shouldReturnProposals() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal1 = buildProposal(200L, requestId);
        ClubProposal proposal2 = buildProposal(201L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(proposal1, proposal2));

        // Act
        List<ClubProposalResponse> response = requestEstablishmentService.getProposals(requestId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    // ==========================================
    // STUDENT OPERATIONS - getProposalDetail (2 test cases)
    // ==========================================

    @Test
    void getProposalDetail_whenValid_shouldReturnProposal() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long proposalId = 200L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal = buildProposal(proposalId, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findByIdAndRequestEstablishmentId(proposalId, requestId))
                .thenReturn(Optional.of(proposal));

        // Act
        ClubProposalResponse response = requestEstablishmentService.getProposalDetail(requestId, proposalId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(proposalId, response.getId());
    }

    @Test
    void getProposalDetail_whenNotOwner_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long proposalId = 200L;
        Long userId = 1L;
        Long otherUserId = 2L;
        RequestEstablishment request = buildRequest(requestId, otherUserId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getProposalDetail(requestId, proposalId, userId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ==========================================
    // STUDENT OPERATIONS - getFinalFormsForStudent (2 test cases)
    // ==========================================

    @Test
    void getFinalFormsForStudent_whenValid_shouldReturnFinalForms() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        ClubCreationFinalForm form1 = buildFinalForm(400L, requestId);
        ClubCreationFinalForm form2 = buildFinalForm(401L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(form1, form2));

        // Act
        List<ClubCreationFinalFormResponse> response = requestEstablishmentService.getFinalFormsForStudent(requestId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void getFinalFormsForStudent_whenNotOwner_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        Long otherUserId = 2L;
        RequestEstablishment request = buildRequest(requestId, otherUserId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getFinalFormsForStudent(requestId, userId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ==========================================
    // SHARED OPERATIONS - getDefenseSchedule (student) (1 test case)
    // ==========================================

    @Test
    void getDefenseSchedule_whenValid_shouldReturnSchedule() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long userId = 1L;
        RequestEstablishment request = buildRequest(requestId, userId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));

        // Act
        DefenseScheduleResponse response = requestEstablishmentService.getDefenseSchedule(requestId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(300L, response.getId());
        assertNotNull(response.getDefenseDate());
    }

    // ==========================================
    // STAFF OPERATIONS - getRequestDetailForStaff (2 test cases)
    // ==========================================

    @Test
    void getRequestDetailForStaff_whenValid_shouldReturnRequest() throws AppException {
        // Arrange
        Long requestId = 100L;
        RequestEstablishment request = buildRequest(requestId, 1L, RequestEstablishmentStatus.SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act
        RequestEstablishmentResponse response = requestEstablishmentService.getRequestDetailForStaff(requestId);

        // Assert
        assertNotNull(response);
        assertEquals(requestId, response.getId());
    }

    @Test
    void getRequestDetailForStaff_whenRequestNotFound_shouldThrowException() {
        // Arrange
        Long requestId = 100L;

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.empty());

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getRequestDetailForStaff(requestId));

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - getProposalsForStaff (2 test cases)
    // ==========================================

    @Test
    void getProposalsForStaff_whenValid_shouldReturnProposals() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal1 = buildProposal(200L, requestId);
        ClubProposal proposal2 = buildProposal(201L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(proposal1, proposal2));

        // Act
        List<ClubProposalResponse> response = requestEstablishmentService.getProposalsForStaff(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.size());
    }

    @Test
    void getProposalsForStaff_whenNotAssigned_shouldThrowForbiddenException() {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        Long otherStaffId = 20L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, otherStaffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));

        // Act & Assert
        AppException ex = assertThrows(AppException.class,
                () -> requestEstablishmentService.getProposalsForStaff(requestId, staffId));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
    }

    // ==========================================
    // STAFF OPERATIONS - getProposalDetailForStaff (1 test case)
    // ==========================================

    @Test
    void getProposalDetailForStaff_whenValid_shouldReturnProposal() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long proposalId = 200L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.PROPOSAL_SUBMITTED);
        ClubProposal proposal = buildProposal(proposalId, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubProposalRepository.findByIdAndRequestEstablishmentId(proposalId, requestId))
                .thenReturn(Optional.of(proposal));

        // Act
        ClubProposalResponse response = requestEstablishmentService.getProposalDetailForStaff(requestId, proposalId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(proposalId, response.getId());
    }

    // ==========================================
    // STAFF OPERATIONS - getDefenseScheduleForStaff (1 test case)
    // ==========================================

    @Test
    void getDefenseScheduleForStaff_whenValid_shouldReturnSchedule() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.DEFENSE_SCHEDULE_PROPOSED);
        DefenseSchedule schedule = buildDefenseSchedule(300L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(defenseScheduleRepository.findByRequestEstablishmentId(requestId)).thenReturn(Optional.of(schedule));

        // Act
        DefenseScheduleResponse response = requestEstablishmentService.getDefenseScheduleForStaff(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(300L, response.getId());
    }

    // ==========================================
    // STAFF OPERATIONS - getFinalFormsForStaff (1 test case)
    // ==========================================

    @Test
    void getFinalFormsForStaff_whenValid_shouldReturnFinalForms() throws AppException {
        // Arrange
        Long requestId = 100L;
        Long staffId = 10L;
        RequestEstablishment request = buildRequestWithStaff(requestId, 1L, staffId, RequestEstablishmentStatus.FINAL_FORM_SUBMITTED);
        ClubCreationFinalForm form1 = buildFinalForm(400L, requestId);

        when(requestEstablishmentRepository.findDetailById(requestId)).thenReturn(Optional.of(request));
        when(clubCreationFinalFormRepository.findAllByRequestEstablishmentIdOrderByCreatedAtDesc(requestId))
                .thenReturn(List.of(form1));

        // Act
        List<ClubCreationFinalFormResponse> response = requestEstablishmentService.getFinalFormsForStaff(requestId, staffId);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
    }
}



