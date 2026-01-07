package com.sep490.backendclubmanagement.unitservice;

import com.sep490.backendclubmanagement.dto.request.*;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.entity.club.ClubRole;
import com.sep490.backendclubmanagement.entity.recruitment.*;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.mapper.RecruitmentApplicationMapper;
import com.sep490.backendclubmanagement.mapper.RecruitmentMapper;
import com.sep490.backendclubmanagement.repository.*;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import com.sep490.backendclubmanagement.service.recruitment.RecruitmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruitmentServiceTest {

    @Mock
    private RecruitmentRepository recruitmentRepository;

    @Mock
    private RecruitmentApplicationRepository applicationRepository;

    @Mock
    private RecruitmentFormQuestionRepository questionRepository;

    @Mock
    private RecruitmentFormAnswerRepository answerRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private TeamOptionRepository teamOptionRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecruitmentMapper recruitmentMapper;

    @Mock
    private RecruitmentApplicationMapper recruitmentApplicationMapper;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ClubMemberShipRepository clubMemberShipRepository;

    @Mock
    private RoleMemberShipRepository roleMembershipRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private ClubRoleRepository clubRoleRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RecruitmentService recruitmentService;

    private Club testClub;
    private User testApplicant;
    private Semester testSemester;
    private Recruitment testRecruitment;
    private RecruitmentApplication testApplication;
    private Team testTeam;
    private RecruitmentData testRecruitmentData;
    private RecruitmentApplicationData testApplicationData;
    private ClubMemberShip testClubMembership;
    private RoleMemberShip testRoleMembership;
    private ClubRole testClubRole;

    private final Long testClubId = 1L;
    private final Long testUserId = 1L;
    private final Long testApplicantId = 2L;
    private final Long testRecruitmentId = 1L;
    private final Long testApplicationId = 1L;
    private final Long testTeamId = 1L;
    private final Long testSemesterId = 1L;

    @BeforeEach
    void setup() {
        // Setup Club
        testClub = Club.builder()
                .id(testClubId)
                .clubName("Test Club")
                .clubCode("TEST001")
                .status("ACTIVE")
                .build();

        // Setup User (Applicant)
        testApplicant = User.builder()
                .id(testApplicantId)
                .email("applicant@fpt.edu.vn")
                .fullName("Test Applicant")
                .studentCode("HE123456")
                .build();

        // Setup Semester
        testSemester = Semester.builder()
                .id(testSemesterId)
                .semesterName("Fall 2024")
                .isCurrent(true)
                .build();

        // Setup Team
        testTeam = Team.builder()
                .id(testTeamId)
                .teamName("Test Team")
                .club(testClub)
                .build();

        // Setup ClubRole
        testClubRole = ClubRole.builder()
                .id(1L)
                .roleName("Member")
                .roleCode("MEMBER")
                .club(testClub)
                .build();

        // Setup Recruitment
        testRecruitment = Recruitment.builder()
                .id(testRecruitmentId)
                .title("Test Recruitment")
                .description("Test Description")
                .status(RecruitmentStatus.OPEN)
                .endDate(LocalDateTime.now().plusDays(7))
                .club(testClub)
                .formQuestions(new HashSet<>())
                .teamOptions(new HashSet<>())
                .build();

        // Setup Recruitment Application
        testApplication = RecruitmentApplication.builder()
                .id(testApplicationId)
                .recruitment(testRecruitment)
                .applicant(testApplicant)
                .teamId(testTeamId)
                .status(RecruitmentApplicationStatus.UNDER_REVIEW)
                .submittedDate(LocalDateTime.now())
                .answers(new HashSet<>())
                .build();

        // Setup ClubMembership
        testClubMembership = ClubMemberShip.builder()
                .id(1L)
                .user(testApplicant)
                .club(testClub)
                .joinDate(LocalDate.now())
                .status(ClubMemberShipStatus.ACTIVE)
                .build();

        // Setup RoleMembership
        testRoleMembership = RoleMemberShip.builder()
                .id(1L)
                .clubMemberShip(testClubMembership)
                .team(testTeam)
                .clubRole(testClubRole)
                .semester(testSemester)
                .isActive(true)
                .build();

        // Setup DTOs
        testRecruitmentData = new RecruitmentData();
        testRecruitmentData.setId(testRecruitmentId);
        testRecruitmentData.setTitle("Test Recruitment");
        testRecruitmentData.setDescription("Test Description");
        testRecruitmentData.setStatus(RecruitmentStatus.OPEN);

        testApplicationData = new RecruitmentApplicationData();
        testApplicationData.setId(testApplicationId);
        testApplicationData.setStatus(RecruitmentApplicationStatus.UNDER_REVIEW);
    }

    // ==================== listRecruitments Tests ====================

    @Test
    void testListRecruitments_Success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Recruitment> recruitments = Arrays.asList(testRecruitment);
        Page<Recruitment> page = new PageImpl<>(recruitments, pageable, 1);

        when(recruitmentRepository.findByClub_Id(testClubId, pageable)).thenReturn(page);
        when(recruitmentMapper.toDtoForList(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        PagedResponse<RecruitmentData> result = recruitmentService.listRecruitments(
                testUserId, testClubId, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recruitmentRepository).findByClub_Id(testClubId, pageable);
    }

    @Test
    void testListRecruitments_WithStatus() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Recruitment> recruitments = Arrays.asList(testRecruitment);
        Page<Recruitment> page = new PageImpl<>(recruitments, pageable, 1);

        when(recruitmentRepository.findByClub_IdAndStatus(testClubId, RecruitmentStatus.OPEN, pageable))
                .thenReturn(page);
        when(recruitmentMapper.toDtoForList(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        PagedResponse<RecruitmentData> result = recruitmentService.listRecruitments(
                testUserId, testClubId, RecruitmentStatus.OPEN, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recruitmentRepository).findByClub_IdAndStatus(testClubId, RecruitmentStatus.OPEN, pageable);
    }

    @Test
    void testListRecruitmentsForGuest_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        List<Recruitment> recruitments = Arrays.asList(testRecruitment);
        Page<Recruitment> page = new PageImpl<>(recruitments, pageable, 1);

        when(recruitmentRepository.findByClub_Id(testClubId, pageable)).thenReturn(page);
        when(recruitmentMapper.toDtoForList(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        PagedResponse<RecruitmentData> result = recruitmentService.listRecruitmentsForGuest(
                testClubId, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(recruitmentRepository).findByClub_Id(testClubId, pageable);
    }

    // ==================== getRecruitment Tests ====================

    @Test
    void testGetRecruitment_Success() throws AppException {
        // Arrange
        when(recruitmentRepository.findByIdWithClub(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        RecruitmentData result = recruitmentService.getRecruitment(testRecruitmentId);

        // Assert
        assertNotNull(result);
        assertEquals(testRecruitmentId, result.getId());
        verify(recruitmentRepository).findByIdWithClub(testRecruitmentId);
        verify(questionRepository).findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId);
        verify(teamOptionRepository).findByRecruitment_Id(testRecruitmentId);
    }

    @Test
    void testGetRecruitment_NotFound() {
        // Arrange
        when(recruitmentRepository.findByIdWithClub(testRecruitmentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AppException.class, () -> {
            recruitmentService.getRecruitment(testRecruitmentId);
        });
    }

    // ==================== createRecruitment Tests ====================

    @Test
    void testCreateRecruitment_Success() throws AppException {
        // Arrange
        RecruitmentQuestionRequest questionRequest = new RecruitmentQuestionRequest();
        questionRequest.questionText = "Why do you want to join?";
        questionRequest.questionType = "TEXT";
        questionRequest.questionOrder = 1;
        questionRequest.isRequired = 1;

        RecruitmentCreateRequest request = new RecruitmentCreateRequest();
        request.title = "New Recruitment";
        request.description = "Description";
        request.endDate = LocalDateTime.now().plusDays(7);
        request.questions = Arrays.asList(questionRequest);
        request.teamOptionIds = Arrays.asList(testTeamId);

        RecruitmentFormQuestion savedQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Why do you want to join?")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(recruitmentMapper.toEntity(any(RecruitmentCreateRequest.class), eq(testClubId)))
                .thenReturn(testRecruitment);
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(testRecruitment);
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Mock question handling
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Collections.emptyList()) // First call for existing questions
                .thenReturn(Arrays.asList(savedQuestion)); // Second call after save
        when(questionRepository.save(any(RecruitmentFormQuestion.class))).thenReturn(savedQuestion);
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(anyList()))
                .thenReturn(Collections.emptyList());

        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        // Mock club officers: include testUserId (creator) and another officer
        Long anotherOfficerId = 99L;
        when(roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(testClubId, testSemesterId))
                .thenReturn(Arrays.asList(testUserId, anotherOfficerId));

        // Act
        RecruitmentData result = recruitmentService.createRecruitment(testUserId, testClubId, request);

        // Assert
        assertNotNull(result);
        verify(clubRepository).findById(testClubId);
        verify(recruitmentRepository).save(any(Recruitment.class));
        verify(questionRepository, atLeastOnce()).save(any(RecruitmentFormQuestion.class));

        // Verify notification was sent to club officers (excluding creator)
        verify(notificationService).sendToUsers(
                argThat(list -> list.size() == 1 && list.contains(anotherOfficerId)), // recipientIds (only anotherOfficerId, not testUserId)
                eq(testUserId), // actorId
                eq("Đợt tuyển thành viên mới đã mở"), // title
                contains("đã mở đợt tuyển thành viên"), // message
                eq(NotificationType.RECRUITMENT_OPENED),
                eq(NotificationPriority.NORMAL),
                contains("/myclub/"), // actionUrl
                eq(testClubId), // relatedClubId
                isNull(), // relatedNewsId
                isNull(), // relatedTeamId
                isNull() // relatedRequestId
        );
    }

    @Test
    void testCreateRecruitment_ClubNotActive() {
        // Arrange
        testClub.setStatus("INACTIVE");
        RecruitmentCreateRequest request = new RecruitmentCreateRequest();
        request.title = "New Recruitment";
        request.endDate = LocalDateTime.now().plusDays(7);
        request.teamOptionIds = Arrays.asList(testTeamId);

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            recruitmentService.createRecruitment(testUserId, testClubId, request);
        });

        assertEquals(ErrorCode.CLUB_NOT_ACTIVE, exception.getErrorCode());
    }

    @Test
    void testCreateRecruitment_EndDateInPast() {
        // Arrange
        RecruitmentCreateRequest request = new RecruitmentCreateRequest();
        request.title = "New Recruitment";
        request.endDate = LocalDateTime.now().minusDays(1); // Past date
        request.teamOptionIds = Arrays.asList(testTeamId);

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            recruitmentService.createRecruitment(testUserId, testClubId, request);
        });

        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    void testCreateRecruitment_WithQuestionOptions() throws AppException {
        // Arrange
        RecruitmentQuestionRequest questionRequest = new RecruitmentQuestionRequest();
        questionRequest.questionText = "Which team are you interested in?";
        questionRequest.questionType = "SINGLE_CHOICE";
        questionRequest.questionOrder = 1;
        questionRequest.isRequired = 1;
        questionRequest.options = Arrays.asList("Technical Team", "Marketing Team", "HR Team");

        RecruitmentCreateRequest request = new RecruitmentCreateRequest();
        request.title = "New Recruitment";
        request.description = "Description";
        request.endDate = LocalDateTime.now().plusDays(7);
        request.questions = Arrays.asList(questionRequest);
        request.teamOptionIds = Arrays.asList(testTeamId);

        RecruitmentFormQuestion savedQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Which team are you interested in?")
                .questionType("SINGLE_CHOICE")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        List<QuestionOption> questionOptions = Arrays.asList(
                QuestionOption.builder().id(1L).optionText("Technical Team").optionOrder(1).question(savedQuestion).build(),
                QuestionOption.builder().id(2L).optionText("Marketing Team").optionOrder(2).question(savedQuestion).build(),
                QuestionOption.builder().id(3L).optionText("HR Team").optionOrder(3).question(savedQuestion).build()
        );

        when(clubRepository.findById(testClubId)).thenReturn(Optional.of(testClub));
        when(recruitmentMapper.toEntity(any(RecruitmentCreateRequest.class), eq(testClubId)))
                .thenReturn(testRecruitment);
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(testRecruitment);
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Mock question handling
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Collections.emptyList()) // First call for existing questions
                .thenReturn(Arrays.asList(savedQuestion)); // Second call after save
        when(questionRepository.save(any(RecruitmentFormQuestion.class))).thenReturn(savedQuestion);
        when(questionOptionRepository.saveAll(anyList())).thenReturn(questionOptions);
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(anyList()))
                .thenReturn(questionOptions);

        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        when(roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(testClubId, testSemesterId))
                .thenReturn(Arrays.asList(testUserId));

        // Act
        RecruitmentData result = recruitmentService.createRecruitment(testUserId, testClubId, request);

        // Assert
        assertNotNull(result);
        verify(clubRepository).findById(testClubId);
        verify(recruitmentRepository).save(any(Recruitment.class));
        verify(questionRepository, atLeastOnce()).save(any(RecruitmentFormQuestion.class));
        verify(questionOptionRepository).saveAll(anyList());
    }

    // ==================== updateRecruitment Tests ====================

    @Test
    void testUpdateRecruitment_Success() throws AppException {
        // Arrange
        RecruitmentQuestionRequest questionRequest = new RecruitmentQuestionRequest();
        questionRequest.id = 1L; // Update existing question
        questionRequest.questionText = "Updated question text";
        questionRequest.questionType = "TEXT";
        questionRequest.questionOrder = 1;
        questionRequest.isRequired = 1;

        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest();
        request.title = "Updated Title";
        request.description = "Updated Description";
        request.endDate = LocalDateTime.now().plusDays(10);
        request.questions = Arrays.asList(questionRequest);
        request.teamOptionIds = Arrays.asList(testTeamId);

        RecruitmentFormQuestion existingQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Old question text")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        RecruitmentFormQuestion updatedQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Updated question text")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(testRecruitment);
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Mock question handling
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(existingQuestion)) // First call for existing questions
                .thenReturn(Arrays.asList(updatedQuestion)); // Second call after save
        when(questionRepository.findById(1L)).thenReturn(Optional.of(existingQuestion));
        when(questionRepository.save(any(RecruitmentFormQuestion.class))).thenReturn(updatedQuestion);
        when(questionOptionRepository.findByQuestion_IdOrderByOptionOrderAsc(1L))
                .thenReturn(Collections.emptyList());
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(anyList()))
                .thenReturn(Collections.emptyList());

        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        RecruitmentData result = recruitmentService.updateRecruitment(testUserId, testRecruitmentId, request);

        // Assert
        assertNotNull(result);
        verify(recruitmentRepository).save(any(Recruitment.class));
        verify(questionRepository, atLeastOnce()).save(any(RecruitmentFormQuestion.class));
    }

    @Test
    void testUpdateRecruitment_DeleteOldQuestionAndAddNew() throws AppException {
        // Arrange
        RecruitmentQuestionRequest newQuestionRequest = new RecruitmentQuestionRequest();
        newQuestionRequest.id = null; // New question (no ID)
        newQuestionRequest.questionText = "New question";
        newQuestionRequest.questionType = "TEXT";
        newQuestionRequest.questionOrder = 1;
        newQuestionRequest.isRequired = 1;

        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest();
        request.title = "Updated Title";
        request.description = "Updated Description";
        request.endDate = LocalDateTime.now().plusDays(10);
        request.questions = Arrays.asList(newQuestionRequest); // Only new question, old one should be deleted
        request.teamOptionIds = Arrays.asList(testTeamId);

        RecruitmentFormQuestion oldQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Old question to be deleted")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        RecruitmentFormQuestion newQuestion = RecruitmentFormQuestion.builder()
                .id(2L)
                .questionText("New question")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(testRecruitment);
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Mock question handling
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(oldQuestion)) // First call shows old question exists
                .thenReturn(Arrays.asList(newQuestion)); // Second call after save shows new question
        when(questionOptionRepository.findByQuestion_IdOrderByOptionOrderAsc(1L))
                .thenReturn(Collections.emptyList()); // Old question has no options
        when(questionRepository.save(any(RecruitmentFormQuestion.class))).thenReturn(newQuestion);
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(anyList()))
                .thenReturn(Collections.emptyList());

        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        RecruitmentData result = recruitmentService.updateRecruitment(testUserId, testRecruitmentId, request);

        // Assert
        assertNotNull(result);
        verify(recruitmentRepository).save(any(Recruitment.class));
        // Since old question has no options, deleteAllById for options is NOT called
        verify(questionOptionRepository, never()).deleteAllById(anyList());
        // Old question should be deleted
        verify(questionRepository).deleteAllById(anyList());
        // New question should be saved
        verify(questionRepository).save(any(RecruitmentFormQuestion.class));
    }

    @Test
    void testUpdateRecruitment_DeleteQuestionWithOptions() throws AppException {
        // Arrange
        RecruitmentQuestionRequest newQuestionRequest = new RecruitmentQuestionRequest();
        newQuestionRequest.id = null; // New question (no ID)
        newQuestionRequest.questionText = "New text question";
        newQuestionRequest.questionType = "TEXT";
        newQuestionRequest.questionOrder = 1;
        newQuestionRequest.isRequired = 1;

        RecruitmentUpdateRequest request = new RecruitmentUpdateRequest();
        request.title = "Updated Title";
        request.description = "Updated Description";
        request.endDate = LocalDateTime.now().plusDays(10);
        request.questions = Arrays.asList(newQuestionRequest); // Only new question, old one with options should be deleted
        request.teamOptionIds = Arrays.asList(testTeamId);

        RecruitmentFormQuestion oldQuestionWithOptions = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Old multiple choice question")
                .questionType("SINGLE_CHOICE")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        QuestionOption option1 = QuestionOption.builder()
                .id(10L)
                .optionText("Option 1")
                .optionOrder(1)
                .question(oldQuestionWithOptions)
                .build();

        QuestionOption option2 = QuestionOption.builder()
                .id(11L)
                .optionText("Option 2")
                .optionOrder(2)
                .question(oldQuestionWithOptions)
                .build();

        RecruitmentFormQuestion newQuestion = RecruitmentFormQuestion.builder()
                .id(2L)
                .questionText("New text question")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(recruitmentRepository.save(any(Recruitment.class))).thenReturn(testRecruitment);
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Mock question handling
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(oldQuestionWithOptions)) // First call shows old question with options exists
                .thenReturn(Arrays.asList(newQuestion)); // Second call after save shows new question
        when(questionOptionRepository.findByQuestion_IdOrderByOptionOrderAsc(1L))
                .thenReturn(Arrays.asList(option1, option2)); // Old question HAS options
        when(questionRepository.save(any(RecruitmentFormQuestion.class))).thenReturn(newQuestion);
        when(questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(anyList()))
                .thenReturn(Collections.emptyList());

        when(teamOptionRepository.findByRecruitment_Id(testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(recruitmentMapper.toDto(any(Recruitment.class))).thenReturn(testRecruitmentData);

        // Act
        RecruitmentData result = recruitmentService.updateRecruitment(testUserId, testRecruitmentId, request);

        // Assert
        assertNotNull(result);
        verify(recruitmentRepository).save(any(Recruitment.class));
        // Old question has options, so both deleteAllById should be called
        verify(questionOptionRepository).deleteAllById(anyList()); // Old question options should be deleted
        verify(questionRepository).deleteAllById(anyList()); // Old question should be deleted
        // New question should be saved
        verify(questionRepository).save(any(RecruitmentFormQuestion.class));
    }

    // ==================== changeRecruitmentStatus Tests ====================

    @Test
    void testChangeRecruitmentStatus_ToOpen() throws AppException {
        // Arrange
        testRecruitment.setStatus(RecruitmentStatus.DRAFT);

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(recruitmentRepository.findByClub_IdAndStatusAndIdNot(testClubId, RecruitmentStatus.OPEN, testRecruitmentId))
                .thenReturn(Collections.emptyList());
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        // Mock club officers: include testUserId (creator) and another officer
        Long anotherOfficerId = 99L;
        when(roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(testClubId, testSemesterId))
                .thenReturn(Arrays.asList(testUserId, anotherOfficerId));

        // Act
        recruitmentService.changeRecruitmentStatus(testUserId, testRecruitmentId, RecruitmentStatus.OPEN);

        // Assert
        verify(recruitmentRepository).save(testRecruitment);
        assertEquals(RecruitmentStatus.OPEN, testRecruitment.getStatus());

        // Verify notification was sent to club officers (excluding creator)
        verify(notificationService).sendToUsers(
                argThat(list -> list.size() == 1 && list.contains(anotherOfficerId)), // recipientIds (only anotherOfficerId, not testUserId)
                eq(testUserId), // actorId
                eq("Đợt tuyển thành viên mới đã mở"), // title
                contains("đã mở đợt tuyển thành viên"), // message
                eq(NotificationType.RECRUITMENT_OPENED),
                eq(NotificationPriority.NORMAL),
                contains("/myclub/"), // actionUrl
                eq(testClubId), // relatedClubId
                isNull(), // relatedNewsId
                isNull(), // relatedTeamId
                isNull() // relatedRequestId
        );
    }

    @Test
    void testChangeRecruitmentStatus_ToClosed() throws AppException {
        // Arrange
        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));

        // Act
        recruitmentService.changeRecruitmentStatus(testUserId, testRecruitmentId, RecruitmentStatus.CLOSED);

        // Assert
        verify(recruitmentRepository).save(testRecruitment);
        assertEquals(RecruitmentStatus.CLOSED, testRecruitment.getStatus());
    }

    // ==================== submitApplication Tests ====================

    @Test
    void testSubmitApplication_Success() throws AppException {
        // Arrange
        RecruitmentFormQuestion requiredQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Why do you want to join?")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        ApplicationSubmitRequest.FormAnswerRequest answerRequest = new ApplicationSubmitRequest.FormAnswerRequest();
        answerRequest.questionId = 1L;
        answerRequest.answerText = "I am passionate about this club";
        answerRequest.hasFile = false;

        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.recruitmentId = testRecruitmentId;
        request.teamId = testTeamId;
        request.answers = Arrays.asList(answerRequest);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "test content".getBytes());

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(userRepository.findById(testApplicantId)).thenReturn(Optional.of(testApplicant));
        when(clubMemberShipRepository.existsByUserIdAndClubId(testApplicantId, testClubId)).thenReturn(false);
        when(applicationRepository.findByApplicant_IdAndRecruitment_Id(testApplicantId, testRecruitmentId))
                .thenReturn(Optional.empty());
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(requiredQuestion));
        when(cloudinaryService.uploadFile(any(MultipartFile.class)))
                .thenReturn(new CloudinaryService.UploadResult("http://cloudinary.com/cv.pdf", "public-id", "pdf", 1024L));
        when(applicationRepository.save(any(RecruitmentApplication.class))).thenReturn(testApplication);
        when(answerRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        when(roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(testClubId, testSemesterId))
                .thenReturn(Collections.emptyList());
        // Mock for getApplicationInternal - using findByIdWithDetails
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.submitApplication(testApplicantId, request, file);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).save(any(RecruitmentApplication.class));
        verify(cloudinaryService).uploadFile(file);
        verify(answerRepository).saveAll(anyList());
    }

    @Test
    void testSubmitApplication_RecruitmentNotOpen() {
        // Arrange
        testRecruitment.setStatus(RecruitmentStatus.CLOSED);
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.recruitmentId = testRecruitmentId;
        request.teamId = testTeamId;

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            recruitmentService.submitApplication(testApplicantId, request, null)
        );

        assertEquals(ErrorCode.RECRUITMENT_CLOSED, exception.getErrorCode());
    }

    @Test
    void testSubmitApplication_AlreadySubmitted() {
        // Arrange
        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.recruitmentId = testRecruitmentId;
        request.teamId = testTeamId;

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(userRepository.findById(testApplicantId)).thenReturn(Optional.of(testApplicant));
        when(clubMemberShipRepository.existsByUserIdAndClubId(testApplicantId, testClubId)).thenReturn(false);
        when(applicationRepository.findByApplicant_IdAndRecruitment_Id(testApplicantId, testRecruitmentId))
                .thenReturn(Optional.of(testApplication));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            recruitmentService.submitApplication(testApplicantId, request, null)
        );

        assertEquals(ErrorCode.ALREADY_APPLIED, exception.getErrorCode());
    }

    @Test
    void testSubmitApplication_RequiredQuestionNotAnswered() {
        // Arrange
        RecruitmentFormQuestion requiredQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Why do you want to join?")
                .questionType("TEXT")
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        ApplicationSubmitRequest.FormAnswerRequest answerRequest = new ApplicationSubmitRequest.FormAnswerRequest();
        answerRequest.questionId = 1L;
        answerRequest.answerText = ""; // Empty answer for required question
        answerRequest.hasFile = false;

        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.recruitmentId = testRecruitmentId;
        request.teamId = testTeamId;
        request.answers = Arrays.asList(answerRequest);

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(userRepository.findById(testApplicantId)).thenReturn(Optional.of(testApplicant));
        when(clubMemberShipRepository.existsByUserIdAndClubId(testApplicantId, testClubId)).thenReturn(false);
        when(applicationRepository.findByApplicant_IdAndRecruitment_Id(testApplicantId, testRecruitmentId))
                .thenReturn(Optional.empty());
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(requiredQuestion));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () ->
            recruitmentService.submitApplication(testApplicantId, request, null)
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void testSubmitApplication_RequiredQuestionAnsweredWithFile() throws AppException {
        // Arrange
        RecruitmentFormQuestion requiredQuestion = RecruitmentFormQuestion.builder()
                .id(1L)
                .questionText("Upload your CV")
                .questionType("FILE"
                )
                .questionOrder(1)
                .isRequired(1)
                .recruitment(testRecruitment)
                .build();

        ApplicationSubmitRequest.FormAnswerRequest answerRequest = new ApplicationSubmitRequest.FormAnswerRequest();
        answerRequest.questionId = 1L;
        answerRequest.answerText = null; // No text, but file will be uploaded
        answerRequest.hasFile = true;

        ApplicationSubmitRequest request = new ApplicationSubmitRequest();
        request.recruitmentId = testRecruitmentId;
        request.teamId = testTeamId;
        request.answers = Arrays.asList(answerRequest);

        MockMultipartFile file = new MockMultipartFile(
                "file", "cv.pdf", "application/pdf", "test content".getBytes());

        when(recruitmentRepository.findById(testRecruitmentId)).thenReturn(Optional.of(testRecruitment));
        when(userRepository.findById(testApplicantId)).thenReturn(Optional.of(testApplicant));
        when(clubMemberShipRepository.existsByUserIdAndClubId(testApplicantId, testClubId)).thenReturn(false);
        when(applicationRepository.findByApplicant_IdAndRecruitment_Id(testApplicantId, testRecruitmentId))
                .thenReturn(Optional.empty());
        when(questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(testRecruitmentId))
                .thenReturn(Arrays.asList(requiredQuestion));
        when(cloudinaryService.uploadFile(any(MultipartFile.class)))
                .thenReturn(new CloudinaryService.UploadResult("http://cloudinary.com/cv.pdf", "public-id", "pdf", 1024L));
        when(applicationRepository.save(any(RecruitmentApplication.class))).thenReturn(testApplication);
        when(answerRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(semesterRepository.findByIsCurrentTrue()).thenReturn(Optional.of(testSemester));
        when(roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(testClubId, testSemesterId))
                .thenReturn(Collections.emptyList());
        // Mock for getApplicationInternal
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.submitApplication(testApplicantId, request, file);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).save(any(RecruitmentApplication.class));
        verify(cloudinaryService).uploadFile(file);
        verify(answerRepository).saveAll(anyList());
    }

    // ==================== listApplications Tests ====================

    @Test
    void testListApplications_Success() throws AppException {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        // Convert page 1 to 0-based page 0
        Pageable adjustedPageable = PageRequest.of(0, 10, pageable.getSort());
        List<RecruitmentApplication> applications = Collections.singletonList(testApplication);
        Page<RecruitmentApplication> page = new PageImpl<>(applications, adjustedPageable, 1);
        RecruitmentApplicationListData listData = new RecruitmentApplicationListData();

        when(applicationRepository.findApplicationsByRecruitment(eq(testRecruitmentId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(recruitmentApplicationMapper.toListDto(any(RecruitmentApplication.class))).thenReturn(listData);
        when(teamRepository.findAllById(anySet())).thenReturn(Collections.singletonList(testTeam));

        // Act
        PagedResponse<RecruitmentApplicationListData> result = recruitmentService.listApplications(
                testUserId, testRecruitmentId, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(applicationRepository).findApplicationsByRecruitment(eq(testRecruitmentId), isNull(), isNull(), any(Pageable.class));
    }

    // ==================== listMyApplications Tests ====================

    @Test
    void testListMyApplications_Success() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        // Convert page 1 to 0-based page 0
        Pageable adjustedPageable = PageRequest.of(0, 10, pageable.getSort());
        List<RecruitmentApplication> applications = Collections.singletonList(testApplication);
        Page<RecruitmentApplication> page = new PageImpl<>(applications, adjustedPageable, 1);
        RecruitmentApplicationListData listData = new RecruitmentApplicationListData();

        when(applicationRepository.findMyApplications(eq(testApplicantId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(recruitmentApplicationMapper.toListDto(any(RecruitmentApplication.class))).thenReturn(listData);
        when(teamRepository.findAllById(anySet())).thenReturn(Collections.singletonList(testTeam));

        // Act
        PagedResponse<RecruitmentApplicationListData> result = recruitmentService.listMyApplications(
                testApplicantId, null, null, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(applicationRepository).findMyApplications(eq(testApplicantId), isNull(), isNull(), any(Pageable.class));
    }

    // ==================== getApplication Tests ====================

    @Test
    void testGetApplication_Success() throws AppException {
        // Arrange
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.getApplication(testUserId, testApplicationId);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).findByIdWithDetails(testApplicationId);
    }

    // ==================== getMyApplication Tests ====================

    @Test
    void testGetMyApplication_Success() throws AppException {
        // Arrange
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.getMyApplication(testApplicantId, testApplicationId);

        // Assert
        assertNotNull(result);
        assertEquals(testApplicationId, result.getId());
        verify(applicationRepository).findByIdWithDetails(testApplicationId);
    }

    @Test
    void testGetMyApplication_NotOwner() {
        // Arrange
        Long otherUserId = 999L;
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> {
            recruitmentService.getMyApplication(otherUserId, testApplicationId);
        });

        assertEquals(ErrorCode.INSUFFICIENT_PERMISSIONS, exception.getErrorCode());
    }

    // ==================== reviewApplication Tests ====================

    @Test
    void testReviewApplication_Accept() throws AppException {
        // Arrange
        ApplicationReviewRequest request = new ApplicationReviewRequest();
        request.applicationId = testApplicationId;
        request.status = RecruitmentApplicationStatus.ACCEPTED;
        request.reviewNotes = "Good candidate";

        when(applicationRepository.findById(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(clubMemberShipRepository.existsByUserIdAndClubId(testApplicantId, testClubId)).thenReturn(false);
        when(clubMemberShipRepository.save(any(ClubMemberShip.class))).thenReturn(testClubMembership);
        when(semesterRepository.findCurrentSemester()).thenReturn(Optional.of(testSemester));
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));
        when(clubRoleRepository.findByClubIdAndRoleCode(testClubId, "MEMBER")).thenReturn(Optional.of(testClubRole));
        when(roleMembershipRepository.save(any(RoleMemberShip.class))).thenReturn(testRoleMembership);
        when(applicationRepository.save(any(RecruitmentApplication.class))).thenReturn(testApplication);
        // Mock for getApplicationInternal - using findByIdWithDetails
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);

        // Act
        RecruitmentApplicationData result = recruitmentService.reviewApplication(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).save(any(RecruitmentApplication.class));
        verify(clubMemberShipRepository).save(any(ClubMemberShip.class));
        verify(roleMembershipRepository).save(any(RoleMemberShip.class));
    }

    @Test
    void testReviewApplication_Reject() throws AppException {
        // Arrange
        ApplicationReviewRequest request = new ApplicationReviewRequest();
        request.applicationId = testApplicationId;
        request.status = RecruitmentApplicationStatus.REJECTED;
        request.reviewNotes = "Not suitable";

        when(applicationRepository.findById(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(RecruitmentApplication.class))).thenReturn(testApplication);
        // Mock for getApplicationInternal - using findByIdWithDetails
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.reviewApplication(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).save(any(RecruitmentApplication.class));
        verify(clubMemberShipRepository, never()).save(any(ClubMemberShip.class));
    }

    // ==================== updateInterviewSchedule Tests ====================

    @Test
    void testUpdateInterviewSchedule_Success() throws AppException {
        // Arrange
        InterviewUpdateRequest request = new InterviewUpdateRequest();
        request.applicationId = testApplicationId;
        request.interviewTime = LocalDateTime.now().plusDays(3);
        request.interviewAddress = "Room 101";
        request.interviewPreparationRequirements = "Please bring your CV";

        when(applicationRepository.findById(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.save(any(RecruitmentApplication.class))).thenReturn(testApplication);
        // Mock for getApplicationInternal - using findByIdWithDetails
        when(applicationRepository.findByIdWithDetails(testApplicationId)).thenReturn(Optional.of(testApplication));
        when(answerRepository.findByApplication_Id(testApplicationId)).thenReturn(Collections.emptyList());
        when(recruitmentApplicationMapper.toDto(any(RecruitmentApplication.class))).thenReturn(testApplicationData);
        when(teamRepository.findById(testTeamId)).thenReturn(Optional.of(testTeam));

        // Act
        RecruitmentApplicationData result = recruitmentService.updateInterviewSchedule(testUserId, request);

        // Assert
        assertNotNull(result);
        verify(applicationRepository).save(any(RecruitmentApplication.class));
        // Note: NotificationService is mocked, so we don't verify the actual notification call
    }

    // ==================== closeExpiredRecruitments Tests ====================

    @Test
    void testCloseExpiredRecruitments_Success() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        when(recruitmentRepository.closeExpiredRecruitments(
                RecruitmentStatus.CLOSED, RecruitmentStatus.OPEN, now))
                .thenReturn(3);

        // Act
        int result = recruitmentService.closeExpiredRecruitments(now);

        // Assert
        assertEquals(3, result);
        verify(recruitmentRepository).closeExpiredRecruitments(
                RecruitmentStatus.CLOSED, RecruitmentStatus.OPEN, now);
    }
}

