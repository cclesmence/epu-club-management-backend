package com.sep490.backendclubmanagement.service.recruitment;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruitmentService implements RecruitmentServiceInterface {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentApplicationRepository applicationRepository;
    private final RecruitmentFormQuestionRepository questionRepository;
    private final RecruitmentFormAnswerRepository answerRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final TeamOptionRepository teamOptionRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final RecruitmentMapper recruitmentMapper;
    private final RecruitmentApplicationMapper recruitmentApplicationMapper;
    private final CloudinaryService cloudinaryService;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final RoleMemberShipRepository roleMembershipRepository;
    private final SemesterRepository semesterRepository;
    private final ClubRoleRepository clubRoleRepository;
    private final ClubRepository clubRepository;
    private final NotificationService notificationService;

    /**
     * Get list of Club Officer user IDs in current semester for a specific club
     * @param clubId Club ID
     * @return List of user IDs who are Club Officers
     */
    private List<Long> getClubOfficersInCurrentSemester(Long clubId) {
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElse(null);

        if (currentSemester == null) {
            return Collections.emptyList();
        }

        return roleMembershipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(
                clubId, currentSemester.getId());
    }

    @Override
    public PagedResponse<RecruitmentData> listRecruitments(Long userId,Long clubId, RecruitmentStatus status,String keyword, Pageable pageable) throws AppException {
        // Permission already checked by @PreAuthorize in controller
        return listRecruitments(clubId, status, keyword, pageable);
    }

    @Override
    public PagedResponse<RecruitmentData> listRecruitmentsForGuest(Long clubId, RecruitmentStatus status, Pageable pageable) {
        return listRecruitments(clubId, status, null, pageable);
    }

    public PagedResponse<RecruitmentData> listRecruitments(Long clubId, RecruitmentStatus status, String keyword, Pageable pageable){
        Page<Recruitment> page;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();

            // Get all records without keyword filter
            Page<Recruitment> allRecruitmentsPage = (status == null)
                    ? recruitmentRepository.findByClub_Id(clubId, PageRequest.of(0, Integer.MAX_VALUE))
                    : recruitmentRepository.findByClub_IdAndStatus(clubId, status, PageRequest.of(0, Integer.MAX_VALUE));

            // Filter using Vietnamese normalization
            List<Recruitment> filteredList = allRecruitmentsPage.getContent().stream()
                    .filter(recruitment -> matchesVietnameseKeyword(trimmedKeyword,
                            recruitment.getTitle(),
                            recruitment.getDescription()))
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<Recruitment> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            page = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            // No keyword filter - use normal database query
            page = (status == null)
                    ? recruitmentRepository.findByClub_Id(clubId, pageable)
                    : recruitmentRepository.findByClub_IdAndStatus(clubId, status, pageable);
        }

        // Use toDtoForList instead of toDto to exclude questions and teamOptions
        Page<RecruitmentData> dataPage = page.map(recruitmentMapper::toDtoForList);
        return PagedResponse.of(dataPage);
    }

    @Override
    public RecruitmentData getRecruitment(Long id) throws AppException {
        // Use optimized query with JOIN FETCH to load club in one query
        Recruitment r = recruitmentRepository.findByIdWithClub(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Load questions + options
        List<RecruitmentFormQuestion> questions = questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(r.getId());

        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream()
                    .map(RecruitmentFormQuestion::getId)
                    .collect(Collectors.toList());
            List<QuestionOption> allOptions = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);

            // Group options by question ID
            Map<Long, List<QuestionOption>> optionsByQuestionId = allOptions.stream()
                    .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

            // Set options for each question
            questions.forEach(question -> {
                List<QuestionOption> options = optionsByQuestionId.getOrDefault(question.getId(), Collections.emptyList());
                question.setOptions(new HashSet<>(options));
            });
        }

        // Load team options
        List<TeamOption> teamOptions = teamOptionRepository.findByRecruitment_Id(r.getId());

        r.setFormQuestions(new HashSet<>(questions));
        r.setTeamOptions(new HashSet<>(teamOptions));

        return recruitmentMapper.toDto(r);
    }

    @Override
    @Transactional
    public RecruitmentData createRecruitment(Long userId, Long clubId, RecruitmentCreateRequest req) throws AppException {
        // Permission already checked by @PreAuthorize in controller

        // Validate club exists and is active
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Validate provided endDate is not in the past
        if (req.endDate != null && req.endDate.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thời hạn không được chọn trong quá khứ");
        }

        Recruitment r = recruitmentMapper.toEntity(req, clubId);
        r = recruitmentRepository.save(r);
        
        // If new recruitment has status OPEN, close all other OPEN recruitments of the club
        if (r.getStatus() == RecruitmentStatus.OPEN) {
            closeOtherOpenRecruitments(clubId, r.getId());
        }
        
        upsertQuestions(r, req.questions);
        upsertTeamOptions(r, req.teamOptionIds);
        
        // Optimized: Load questions with options in batch to avoid N+1 queries
        List<RecruitmentFormQuestion> questions = questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(r.getId());
        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream().map(RecruitmentFormQuestion::getId).collect(Collectors.toList());
            List<QuestionOption> allOptions = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);

            // Group options by question ID
            Map<Long, List<QuestionOption>> optionsByQuestionId = allOptions.stream()
                    .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

            // Set options for each question
            questions.forEach(question -> {
                List<QuestionOption> options = optionsByQuestionId.getOrDefault(question.getId(), Collections.emptyList());
                question.setOptions(new HashSet<>(options));
            });
        }
        r.setFormQuestions(new HashSet<>(questions));

        // Load team options
        List<TeamOption> teamOptions = teamOptionRepository.findByRecruitment_Id(r.getId());
        r.setTeamOptions(new HashSet<>(teamOptions));

        // Send notification to club officers when recruitment is opened
        if (r.getStatus() == RecruitmentStatus.OPEN) {
            try {
                // Get Club Officers in current semester
                List<Long> officerIds = getClubOfficersInCurrentSemester(clubId);
                List<Long> recipientIds = officerIds.stream()
                        .filter(memberId -> !memberId.equals(userId)) // Don't notify the creator
                        .collect(Collectors.toList());

                if (!recipientIds.isEmpty()) {
                    String actionUrl = "/myclub/" + clubId + "/recruitments";
                    String title = "Đợt tuyển thành viên mới đã mở";
                    String message = "CLB " + club.getClubName() + " đã mở đợt tuyển thành viên: \"" + r.getTitle() + "\"";

                    notificationService.sendToUsers(
                            recipientIds,
                            userId,
                            title,
                            message,
                            NotificationType.RECRUITMENT_OPENED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            clubId,
                            null,
                            null,
                            null
                    );
                }
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Failed to send recruitment opened notification: " + e.getMessage());
            }
        }

        return recruitmentMapper.toDto(r);
    }

    @Override
    @Transactional
    public RecruitmentData updateRecruitment(Long userId, Long id, RecruitmentUpdateRequest req) throws AppException {
        Recruitment r = recruitmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Permission already checked by @PreAuthorize in controller

        // Check if club is active
        if (!"ACTIVE".equalsIgnoreCase(r.getClub().getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Check if recruitment is closed
        if (r.getStatus() == RecruitmentStatus.CLOSED) {
            throw new AppException(ErrorCode.RECRUITMENT_CLOSED);
        }
        
        // Check if recruitment end date has passed
        if (r.getEndDate() != null && LocalDateTime.now().isAfter(r.getEndDate())) {
            throw new AppException(ErrorCode.RECRUITMENT_ENDED);
        }

        // Prevent updating recruitment to an end date in the past
        if (req.endDate != null && req.endDate.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thời hạn không được chọn trong quá khứ");
        }

        recruitmentMapper.updateEntity(r, req);
        
        // If recruitment is updated to OPEN, close all other OPEN recruitments of the club
        if (r.getStatus() == RecruitmentStatus.OPEN) {
            closeOtherOpenRecruitments(r.getClub().getId(), r.getId());
        }
        
        recruitmentRepository.save(r);
        upsertQuestions(r, req.questions);
        upsertTeamOptions(r, req.teamOptionIds);
        
        // Optimized: Load questions with options in batch to avoid N+1 queries
        List<RecruitmentFormQuestion> questions = questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(r.getId());
        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream().map(RecruitmentFormQuestion::getId).collect(Collectors.toList());
            List<QuestionOption> allOptions = questionOptionRepository.findByQuestionIdInOrderByQuestionIdAscOptionOrderAsc(questionIds);

            // Group options by question ID
            Map<Long, List<QuestionOption>> optionsByQuestionId = allOptions.stream()
                    .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));

            // Set options for each question
            questions.forEach(question -> {
                List<QuestionOption> options = optionsByQuestionId.getOrDefault(question.getId(), Collections.emptyList());
                question.setOptions(new HashSet<>(options));
            });
        }
        r.setFormQuestions(new HashSet<>(questions));

        // Load team options
        List<TeamOption> teamOptions = teamOptionRepository.findByRecruitment_Id(r.getId());
        r.setTeamOptions(new HashSet<>(teamOptions));

        return recruitmentMapper.toDto(r);
    }

    @Override
    @Transactional
    public void changeRecruitmentStatus(Long userId, Long id, RecruitmentStatus status) throws AppException {
        Recruitment r = recruitmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Permission already checked by @PreAuthorize in controller

        // Check if club is active
        if (!"ACTIVE".equalsIgnoreCase(r.getClub().getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Store old status to check if status changed to OPEN
        RecruitmentStatus oldStatus = r.getStatus();

        // If new status is OPEN, close all other OPEN recruitments of the club
        if (status == RecruitmentStatus.OPEN) {
            closeOtherOpenRecruitments(r.getClub().getId(), r.getId());
        }
        
        r.setStatus(status);
        recruitmentRepository.save(r);

        // Send notification to Club Officers if status changed to OPEN
        if (status == RecruitmentStatus.OPEN && oldStatus != RecruitmentStatus.OPEN) {
            try {
                Club club = r.getClub();
                Long clubId = club.getId();

                // Get Club Officers in current semester
                List<Long> officerIds = getClubOfficersInCurrentSemester(clubId);
                List<Long> recipientIds = officerIds.stream()
                        .filter(memberId -> !memberId.equals(userId)) // Don't notify the creator
                        .collect(Collectors.toList());

                if (!recipientIds.isEmpty()) {
                    String actionUrl = "/myclub/" + club.getId() + "/recruitments";
                    String title = "Đợt tuyển thành viên mới đã mở";
                    String message = "CLB " + club.getClubName() + " đã mở đợt tuyển thành viên: \"" + r.getTitle() + "\"";

                    // Use  notification to avoid blocking
                    notificationService.sendToUsers(
                            recipientIds,
                            userId,
                            title,
                            message,
                            NotificationType.RECRUITMENT_OPENED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            clubId,
                            null,
                            null,
                            null
                    );
                }
            } catch (Exception e) {
                // Log error but don't fail the operation
                System.err.println("Failed to send recruitment status change notification: " + e.getMessage());
            }
        }
    }


    
    public PagedResponse<RecruitmentApplicationListData> listApplications(Long userId, Long recruitmentId, RecruitmentApplicationStatus status, String keyword, Pageable pageable) throws AppException {
        // Permission already checked by @PreAuthorize in controller

        // Convert from 1-based page (frontend) to 0-based page (database)
        int requestedPage = pageable.getPageNumber();
        int zeroBasedPage = Math.max(0, requestedPage - 1); // Ensure non-negative
        Pageable adjustedPageable = PageRequest.of(zeroBasedPage, pageable.getPageSize(), pageable.getSort());

        Page<RecruitmentApplication> page;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();

            // Get all records without keyword filter
            Page<RecruitmentApplication> allApplicationsPage =
                applicationRepository.findApplicationsByRecruitment(recruitmentId, status, null, PageRequest.of(0, Integer.MAX_VALUE));

            // Filter using Vietnamese normalization
            List<RecruitmentApplication> filteredList = allApplicationsPage.getContent().stream()
                    .filter(app -> matchesVietnameseKeyword(trimmedKeyword,
                            app.getApplicant() != null ? app.getApplicant().getFullName() : "",
                            app.getApplicant() != null ? app.getApplicant().getEmail() : "",
                            app.getApplicant() != null ? app.getApplicant().getStudentCode() : ""))
                    .toList();

            // Apply pagination manually
            int start = (int) adjustedPageable.getOffset();
            int end = Math.min((start + adjustedPageable.getPageSize()), filteredList.size());
            List<RecruitmentApplication> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            page = new PageImpl<>(paginatedList, adjustedPageable, filteredList.size());
        } else {
            // No keyword filter - use normal database query
            page = applicationRepository.findApplicationsByRecruitment(recruitmentId, status, null, adjustedPageable);
        }

        // Batch load team names to avoid N+1 queries
        Set<Long> teamIds = page.getContent().stream()
                .map(RecruitmentApplication::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> teamNameMap = new HashMap<>();
        if (!teamIds.isEmpty()) {
            teamRepository.findAllById(teamIds).forEach(team ->
                teamNameMap.put(team.getId(), team.getTeamName())
            );
        }

        // Map to lightweight DTO with batch-loaded team names (no answers)
        Page<RecruitmentApplicationListData> dataPage = page.map(app -> {
            RecruitmentApplicationListData data = recruitmentApplicationMapper.toListDto(app);
            if (app.getTeamId() != null) {
                data.setTeamName(teamNameMap.get(app.getTeamId()));
            }
            return data;
        });
        return PagedResponse.of(dataPage);
    }

    @Override
    public PagedResponse<RecruitmentApplicationListData> listMyApplications(Long applicantId, RecruitmentApplicationStatus status, String keyword, Pageable pageable) {
        // Convert from 1-based page (frontend) to 0-based page (database)
        int requestedPage = pageable.getPageNumber();
        int zeroBasedPage = Math.max(0, requestedPage - 1); // Ensure non-negative
        Pageable adjustedPageable = PageRequest.of(zeroBasedPage, pageable.getPageSize(), pageable.getSort());

        Page<RecruitmentApplication> page;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();

            // Get all records without keyword filter
            Page<RecruitmentApplication> allApplicationsPage =
                applicationRepository.findMyApplications(applicantId, status, null, PageRequest.of(0, Integer.MAX_VALUE));

            // Filter using Vietnamese normalization
            List<RecruitmentApplication> filteredList = allApplicationsPage.getContent().stream()
                    .filter(app -> matchesVietnameseKeyword(trimmedKeyword,
                            app.getRecruitment() != null ? app.getRecruitment().getTitle() : "",
                            app.getRecruitment() != null && app.getRecruitment().getClub() != null ?
                                app.getRecruitment().getClub().getClubName() : ""))
                    .toList();

            // Apply pagination manually
            int start = (int) adjustedPageable.getOffset();
            int end = Math.min((start + adjustedPageable.getPageSize()), filteredList.size());
            List<RecruitmentApplication> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            page = new PageImpl<>(paginatedList, adjustedPageable, filteredList.size());
        } else {
            // No keyword filter - use normal database query
            page = applicationRepository.findMyApplications(applicantId, status, null, adjustedPageable);
        }

        // Batch load team names to avoid N+1 queries
        Set<Long> teamIds = page.getContent().stream()
                .map(RecruitmentApplication::getTeamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> teamNameMap = new HashMap<>();
        if (!teamIds.isEmpty()) {
            teamRepository.findAllById(teamIds).forEach(team ->
                teamNameMap.put(team.getId(), team.getTeamName())
            );
        }

        // Map to lightweight DTO with batch-loaded team names (no answers)
        Page<RecruitmentApplicationListData> dataPage = page.map(app -> {
            RecruitmentApplicationListData data = recruitmentApplicationMapper.toListDto(app);
            if (app.getTeamId() != null) {
                data.setTeamName(teamNameMap.get(app.getTeamId()));
            }
            // Only show reviewNotes when status is ACCEPTED or REJECTED
            if (app.getStatus() != RecruitmentApplicationStatus.ACCEPTED &&
                app.getStatus() != RecruitmentApplicationStatus.REJECTED) {
                data.setReviewNotes(null);
            }
            return data;
        });
        return PagedResponse.of(dataPage);
    }


    /**
     * Submit application with file upload support
     * @param applicantId User ID of the applicant
     * @param req Application submit request
     * @param file Single file to upload (optional)
     * @return Submitted application data
     * @throws AppException if submission fails
     */
    @Transactional
    public RecruitmentApplicationData submitApplication(Long applicantId, ApplicationSubmitRequest req, MultipartFile file) throws AppException {
        Recruitment recruitment = recruitmentRepository.findById(req.recruitmentId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        // Do not allow submission if recruitment is not OPEN (e.g., DRAFT or CLOSED)
        if (recruitment.getStatus() != RecruitmentStatus.OPEN) {
            throw new AppException(ErrorCode.RECRUITMENT_CLOSED);
        }

        // Check if club is active (only active clubs can receive applications)
        Club club = recruitment.getClub();
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Validate file size (max 20 MB)
        final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20 MB
        if (file != null && !file.isEmpty() && file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Kích thước tập tin vượt quá giới hạn 20MB");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));

        // Check if user is already an active member of the club
        Long clubId = club.getId();
        boolean isAlreadyMember = clubMemberShipRepository.existsByUserIdAndClubId(applicantId, clubId);
        if (isAlreadyMember) {
            throw new AppException(ErrorCode.ALREADY_CLUB_MEMBER);
        }

        // Check if user has already submitted an application for this recruitment period
        if (applicationRepository.findByApplicant_IdAndRecruitment_Id(applicantId, recruitment.getId()).isPresent()) {
            throw new AppException(ErrorCode.ALREADY_APPLIED);
        }

        // Validate required questions are answered
        List<RecruitmentFormQuestion> questions = questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(recruitment.getId());

        // Build a map of answered question IDs for faster lookup
        Map<Long, ApplicationSubmitRequest.FormAnswerRequest> answerMap = req.answers.stream()
                .collect(Collectors.toMap(ans -> ans.questionId, ans -> ans));

        for (RecruitmentFormQuestion question : questions) {
            if (question.getIsRequired() != null && question.getIsRequired() == 1) {
                ApplicationSubmitRequest.FormAnswerRequest answer = answerMap.get(question.getId());

                boolean isAnswered = false;
                if (answer != null) {
                    // Check if answer has text
                    if (answer.answerText != null && !answer.answerText.trim().isEmpty()) {
                        isAnswered = true;
                    }
                    // Check if file is uploaded for this answer
                    else if (file != null && !file.isEmpty() && answer.hasFile != null && answer.hasFile) {
                        isAnswered = true;
                    }
                }

                if (!isAnswered) {
                    throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
                }
            }
        }

        RecruitmentApplication app = RecruitmentApplication.builder()
                .recruitment(recruitment)
                .applicant(applicant)
                .teamId(req.teamId)
                .status(RecruitmentApplicationStatus.UNDER_REVIEW)
                .submittedDate(LocalDateTime.now())
                .build();
        app = applicationRepository.save(app);

        // Upload file if provided
        String uploadedFileUrl = null;
        if (file != null && !file.isEmpty()) {
            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadFile(file);
                uploadedFileUrl = uploadResult.url();
            } catch (Exception e) {
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        // Save answers
        List<RecruitmentFormAnswer> answers = new ArrayList<>();
        for (ApplicationSubmitRequest.FormAnswerRequest a : req.answers) {
            String fileUrl = null;

            // If file was uploaded and this answer is marked to use it, assign the uploaded URL
            if (uploadedFileUrl != null && a.hasFile != null && a.hasFile) {
                fileUrl = uploadedFileUrl;
            }

            RecruitmentFormAnswer ans = RecruitmentFormAnswer.builder()
                    .application(app)
                    .question(RecruitmentFormQuestion.builder().id(a.questionId).build())
                    .answerText(a.answerText)
                    .fileUrl(fileUrl)
                    .build();
            answers.add(ans);
        }
        answerRepository.saveAll(answers);

        // Send notification to Club Officers about new application
        try {
            List<Long> officerIds = getClubOfficersInCurrentSemester(clubId);

            if (!officerIds.isEmpty()) {
                String actionUrl = "/myclub/" + club.getId() + "/recruitments";
                String title = "Có đơn ứng tuyển mới";
                String message = applicant.getFullName() + " đã nộp đơn ứng tuyển vào đợt tuyển thành viên: \""
                        + recruitment.getTitle() + "\"";

                // Use  notification to avoid blocking
                notificationService.sendToUsers(
                        officerIds,
                        applicantId,
                        title,
                        message,
                        NotificationType.RECRUITMENT_APPLICATION_SUBMITTED,
                        NotificationPriority.NORMAL,
                        actionUrl,
                        clubId,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send application submitted notification: " + e.getMessage());
        }

        return getApplicationInternal(app.getId());
    }

    @Override
    public RecruitmentApplicationData getApplication(Long userId, Long applicationId) throws AppException {
        // Use optimized query with JOIN FETCH to load relationships in one query
        RecruitmentApplication app = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Permission already checked by @PreAuthorize in controller

        List<RecruitmentFormAnswer> answers = answerRepository.findByApplication_Id(applicationId);
        app.setAnswers(new HashSet<>(answers));
        
        RecruitmentApplicationData data = recruitmentApplicationMapper.toDto(app);
        setTeamName(data, app.getTeamId());

        return data;
    }

    @Override
    public RecruitmentApplicationData getMyApplication(Long applicantId, Long applicationId) throws AppException {
        // Use optimized query with JOIN FETCH to load relationships in one query
        RecruitmentApplication app = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Check: application must belong to the applicant
        if (!app.getApplicant().getId().equals(applicantId)) {
            throw new AppException(ErrorCode.INSUFFICIENT_PERMISSIONS);
        }
        
        List<RecruitmentFormAnswer> answers = answerRepository.findByApplication_Id(applicationId);
        app.setAnswers(new HashSet<>(answers));
        
        RecruitmentApplicationData data = recruitmentApplicationMapper.toDto(app);
        setTeamName(data, app.getTeamId());

        // Only show reviewNotes when status is ACCEPTED or REJECTED
        if (app.getStatus() != RecruitmentApplicationStatus.ACCEPTED &&
            app.getStatus() != RecruitmentApplicationStatus.REJECTED) {
            data.setReviewNotes(null);
        }

        return data;
    }

    @Override
    @Transactional
    public RecruitmentApplicationData reviewApplication(Long userId, ApplicationReviewRequest req) throws AppException {
        RecruitmentApplication app = applicationRepository.findById(req.applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Permission already checked by @PreAuthorize in controller

        // Check if club is active
        Club club = app.getRecruitment().getClub();
        Long clubId = club.getId();
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Check if current status is INTERVIEW and interview time hasn't passed yet
        if (app.getStatus() == RecruitmentApplicationStatus.INTERVIEW
            && app.getInterviewTime() != null
            && LocalDateTime.now().isBefore(app.getInterviewTime())) {
            throw new AppException(ErrorCode.INTERVIEW_NOT_YET);
        }

        app.setStatus(req.status);
        app.setReviewNotes(req.reviewNotes);
        app.setReviewedDate(LocalDateTime.now());
        app.setInterviewTime(req.interviewTime);
        app.setInterviewAddress(req.interviewAddress);
        app.setInterviewPreparationRequirements(req.interviewPreparationRequirements);
        applicationRepository.save(app);
        
        // If status is ACCEPTED, add user to the registered team
        if (req.status == RecruitmentApplicationStatus.ACCEPTED && app.getTeamId() != null) {
            addMemberToTeam(app);
        }
        
        // Send notification to applicant about application review result
        try {
            Long applicantId = app.getApplicant().getId();
            String actionUrl = "/myRecruitmentApplications";
            String title = "";
            String message = "";
            NotificationType notificationType = null;
            NotificationPriority priority = NotificationPriority.NORMAL;

            if (req.status == RecruitmentApplicationStatus.ACCEPTED) {
                title = "Đơn ứng tuyển được chấp nhận";
                message = "Đơn ứng tuyển của bạn vào đợt tuyển \"" + app.getRecruitment().getTitle()
                        + "\" đã được chấp nhận. Chào mừng bạn đến với CLB " + club.getClubName() + "!";
                notificationType = NotificationType.RECRUITMENT_APPLICATION_APPROVED;
                priority = NotificationPriority.HIGH;
            } else if (req.status == RecruitmentApplicationStatus.REJECTED) {
                title = "Đơn ứng tuyển không được chấp nhận";
                message = "Đơn ứng tuyển của bạn vào đợt tuyển \"" + app.getRecruitment().getTitle()
                        + "\" không được chấp nhận.";
                if (req.reviewNotes != null && !req.reviewNotes.trim().isEmpty()) {
                    message += " Lý do: " + req.reviewNotes;
                }
                notificationType = NotificationType.RECRUITMENT_APPLICATION_REJECTED;
            } else if (req.interviewTime != null) {
                // If interview time is scheduled (regardless of status)
                title = "Thông báo lịch phỏng vấn";
                message = "Bạn đã được mời phỏng vấn cho đợt tuyển \"" + app.getRecruitment().getTitle() + "\".";
                if (req.interviewAddress != null && !req.interviewAddress.trim().isEmpty()) {
                    message += " Địa điểm: " + req.interviewAddress + ".";
                }
                if (req.interviewPreparationRequirements != null && !req.interviewPreparationRequirements.trim().isEmpty()) {
                    message += " Yêu cầu chuẩn bị: " + req.interviewPreparationRequirements;
                }
                notificationType = NotificationType.RECRUITMENT_APPLICATION_REVIEWED;
                priority = NotificationPriority.HIGH;
            }

            if (notificationType != null) {
                // Use  notification to avoid blocking
                notificationService.sendToUsers(
                        Collections.singletonList(applicantId), // Wrap single user in list for 
                        userId,
                        title,
                        message,
                        notificationType,
                        priority,
                        actionUrl,
                        clubId,
                        null,
                        null,
                        null
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send application review notification: " + e.getMessage());
        }

        return getApplicationInternal(app.getId());
    }

    @Override
    @Transactional
    public RecruitmentApplicationData updateInterviewSchedule(Long userId, InterviewUpdateRequest req) throws AppException {
        RecruitmentApplication app = applicationRepository.findById(req.applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));

        // Permission already checked by @PreAuthorize in controller

        // Check if club is active
        Club club = app.getRecruitment().getClub();
        Long clubId = club.getId();
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Check if current interview time hasn't passed yet (can only update before interview time)
        if (app.getInterviewTime() != null && LocalDateTime.now().isAfter(app.getInterviewTime())) {
            throw new AppException(ErrorCode.INTERVIEW_TIME_PASSED);
        }

        // Prevent updating recruitment to an end date in the past
        if (req.interviewTime != null && req.interviewTime.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Thời gian phỏng vấn không được chọn trong quá khứ");
        }

        // Update interview schedule
        app.setInterviewTime(req.interviewTime);
        app.setInterviewAddress(req.interviewAddress);
        app.setInterviewPreparationRequirements(req.interviewPreparationRequirements);
        applicationRepository.save(app);

        // Send notification to applicant about interview schedule update
        try {
            Long applicantId = app.getApplicant().getId();
            String actionUrl = "/myRecruitmentApplications";
            String title = "Cập nhật lịch phỏng vấn";
            String message = "Lịch phỏng vấn cho đợt tuyển \"" + app.getRecruitment().getTitle() + "\" đã được cập nhật.";

            if (req.interviewTime != null) {
                message += " Thời gian: " + req.interviewTime + ".";
            }
            if (req.interviewAddress != null && !req.interviewAddress.trim().isEmpty()) {
                message += " Địa điểm: " + req.interviewAddress + ".";
            }
            if (req.interviewPreparationRequirements != null && !req.interviewPreparationRequirements.trim().isEmpty()) {
                message += " Yêu cầu chuẩn bị: " + req.interviewPreparationRequirements;
            }

            // Use  notification to avoid blocking
            notificationService.sendToUsers(
                    Collections.singletonList(applicantId), // Wrap single user in list for 
                    userId,
                    title,
                    message,
                    NotificationType.RECRUITMENT_APPLICATION_REVIEWED,
                    NotificationPriority.HIGH,
                    actionUrl,
                    clubId,
                    null,
                    null,
                    null
            );
        } catch (Exception e) {
            // Log error but don't fail the operation
            System.err.println("Failed to send interview schedule update notification: " + e.getMessage());
        }

        return getApplicationInternal(app.getId());
    }
    
    /**
     * Get application information without permission check (for internal use)
     * Optimized to reduce queries by using JOIN FETCH
     */
    private RecruitmentApplicationData getApplicationInternal(Long applicationId) throws AppException {
        // Use optimized query with JOIN FETCH to load all relationships in one query
        RecruitmentApplication app = applicationRepository.findByIdWithDetails(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        List<RecruitmentFormAnswer> answers = answerRepository.findByApplication_Id(applicationId);
        app.setAnswers(new HashSet<>(answers));
        
        RecruitmentApplicationData data = recruitmentApplicationMapper.toDto(app);

        // Optimized: Only query team if teamId exists
        if (app.getTeamId() != null) {
            teamRepository.findById(app.getTeamId()).ifPresent(team ->
                data.setTeamName(team.getTeamName())
            );
        }

        return data;
    }

    /**
     * Helper method to set team name in application data
     * Optimized to avoid unnecessary queries
     */
    private void setTeamName(RecruitmentApplicationData data, Long teamId) {
        if (teamId != null) {
            teamRepository.findById(teamId).ifPresent(team ->
                data.setTeamName(team.getTeamName())
            );
        }
    }
    
    /**
     * Add member to team after application is accepted
     * Only adds the user if they are not already a member of the club (regardless of status)
     */
    private void addMemberToTeam(RecruitmentApplication app) throws AppException {
        Long applicantId = app.getApplicant().getId();
        Long clubId = app.getRecruitment().getClub().getId();
        Long teamId = app.getTeamId();
        
        // Check if user is already a member of the club (regardless of status)
        boolean isAlreadyMember = clubMemberShipRepository
                .existsByUserIdAndClubId(applicantId, clubId);

        // Only proceed if user is not already a member
        if (isAlreadyMember) {
            // User is already a member of the club, do not add again
            return;
        }

        // User is not a member yet, proceed to add them
        Club club = app.getRecruitment().getClub();
        User applicant = app.getApplicant();

        // Create new ClubMemberShip
        ClubMemberShip clubMembership = ClubMemberShip.builder()
                .user(applicant)
                .club(club)
                .joinDate(LocalDate.now())
                .status(ClubMemberShipStatus.ACTIVE)
                .build();
        clubMembership = clubMemberShipRepository.save(clubMembership);

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Get team
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_SERVER_ERROR));
        
        // Find ClubRole with role code MEMBER
        ClubRole memberRole = clubRoleRepository.findByClubIdAndRoleCode(clubId, "MEMBER")
                .orElse(null); // If not found, set to null (keep original behavior)
        
        // Create RoleMemberShip to assign user to team
        RoleMemberShip roleMembership = RoleMemberShip.builder()
                .clubMemberShip(clubMembership)
                .team(team)
                .clubRole(memberRole) // Assign MEMBER role if found
                .semester(currentSemester)
                .isActive(true)
                .build();
        roleMembershipRepository.save(roleMembership);
    }


    private void upsertQuestions(Recruitment recruitment, List<RecruitmentQuestionRequest> reqs) {
        if (reqs == null) return;
        
        // Get existing questions
        List<RecruitmentFormQuestion> existingQuestions = questionRepository.findByRecruitment_IdOrderByQuestionOrderAsc(recruitment.getId());
        
        // Collect IDs of questions that should be kept
        Set<Long> requestedQuestionIds = reqs.stream()
                .map(q -> q.id)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        
        // Batch delete questions that are not in the request (orphaned questions)
        List<Long> questionsToDelete = new ArrayList<>();
        List<Long> optionsToDelete = new ArrayList<>();

        for (RecruitmentFormQuestion existingQuestion : existingQuestions) {
            if (!requestedQuestionIds.contains(existingQuestion.getId())) {
                // Collect option IDs to delete
                questionOptionRepository.findByQuestion_IdOrderByOptionOrderAsc(existingQuestion.getId())
                        .forEach(option -> optionsToDelete.add(option.getId()));
                questionsToDelete.add(existingQuestion.getId());
            }
        }

        // Batch delete options and questions
        if (!optionsToDelete.isEmpty()) {
            questionOptionRepository.deleteAllById(optionsToDelete);
        }
        if (!questionsToDelete.isEmpty()) {
            questionRepository.deleteAllById(questionsToDelete);
        }

        // Create or update questions
        for (RecruitmentQuestionRequest q : reqs) {
            RecruitmentFormQuestion entity;
            
            if (q.id != null) {
                // UPDATE existing question
                entity = questionRepository.findById(q.id)
                        .orElseThrow(() -> new RuntimeException("Question not found: " + q.id));
                entity.setQuestionText(q.questionText);
                entity.setQuestionType(q.questionType);
                entity.setQuestionOrder(q.questionOrder);
                entity.setIsRequired(q.isRequired);
                entity = questionRepository.save(entity);
                
                // Batch delete old options
                List<Long> oldOptionIds = questionOptionRepository.findByQuestion_IdOrderByOptionOrderAsc(entity.getId())
                        .stream().map(QuestionOption::getId).collect(Collectors.toList());
                if (!oldOptionIds.isEmpty()) {
                    questionOptionRepository.deleteAllById(oldOptionIds);
                }
            } else {
                // CREATE new question
                entity = RecruitmentFormQuestion.builder()
                        .questionText(q.questionText)
                        .questionType(q.questionType)
                        .questionOrder(q.questionOrder)
                        .isRequired(q.isRequired)
                        .recruitment(recruitment)
                        .build();
                entity = questionRepository.save(entity);
            }
            
            // Save question options if provided
            if (q.options != null && !q.options.isEmpty()) {
                saveQuestionOptions(entity, q.options);
            }
        }
    }

    private void saveQuestionOptions(RecruitmentFormQuestion question, List<String> options) {
        List<QuestionOption> optionEntities = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            QuestionOption option = QuestionOption.builder()
                    .optionText(options.get(i))
                    .optionOrder(i + 1)
                    .question(question)
                    .build();
            optionEntities.add(option);
        }
        // Batch save all options at once
        questionOptionRepository.saveAll(optionEntities);
    }

    private void upsertTeamOptions(Recruitment recruitment, List<Long> teamIds) {
        // Validate teamIds is not null or empty (should be enforced by validation, but double-check)
        if (teamIds == null || teamIds.isEmpty()) {
            throw new RuntimeException("teamOptionIds cannot be empty. Must select at least one team.");
        }
        
        // Get existing team options
        List<TeamOption> existingTeamOptions = teamOptionRepository.findByRecruitment_Id(recruitment.getId());
        
        // Extract existing team IDs
        Set<Long> existingTeamIds = existingTeamOptions.stream()
                .map(teamOption -> teamOption.getTeam().getId())
                .collect(java.util.stream.Collectors.toSet());
        
        // Convert request team IDs to set
        Set<Long> requestedTeamIds = new java.util.HashSet<>(teamIds);
        
        // Batch delete team options that are not in the request (orphaned team options)
        List<Long> teamOptionsToDelete = existingTeamOptions.stream()
                .filter(teamOption -> !requestedTeamIds.contains(teamOption.getTeam().getId()))
                .map(TeamOption::getId)
                .collect(Collectors.toList());

        if (!teamOptionsToDelete.isEmpty()) {
            teamOptionRepository.deleteAllById(teamOptionsToDelete);
        }
        
        // Batch create new team options that are not in existing
        List<TeamOption> newTeamOptions = new ArrayList<>();
        for (Long teamId : requestedTeamIds) {
            if (!existingTeamIds.contains(teamId)) {
                // Verify team exists
                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new RuntimeException("Team not found: " + teamId));
                
                TeamOption teamOption = TeamOption.builder()
                        .recruitment(recruitment)
                        .team(team)
                        .build();
                newTeamOptions.add(teamOption);
            }
        }

        if (!newTeamOptions.isEmpty()) {
            teamOptionRepository.saveAll(newTeamOptions);
        }
    }

    /**
     * Close all other OPEN recruitments of the club (except current recruitment)
     * to ensure only one recruitment is OPEN at a time
     */
    private void closeOtherOpenRecruitments(Long clubId, Long currentRecruitmentId) {
        List<Recruitment> openRecruitments = recruitmentRepository.findByClub_IdAndStatusAndIdNot(
                clubId, RecruitmentStatus.OPEN, currentRecruitmentId
        );
        
        if (!openRecruitments.isEmpty()) {
            // Batch update all recruitments at once
            openRecruitments.forEach(recruitment -> recruitment.setStatus(RecruitmentStatus.CLOSED));
            recruitmentRepository.saveAll(openRecruitments);
        }
    }



    /**
     * Close expired recruitments whose endDate is before the provided time.
     * Returns the number of recruitments updated.
     */
    @Transactional
    public int closeExpiredRecruitments(java.time.LocalDateTime now) {
        // Only close recruitments that are currently OPEN
        return recruitmentRepository.closeExpiredRecruitments(RecruitmentStatus.CLOSED, RecruitmentStatus.OPEN, now);
    }

    private String normalizeVietnamese(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("đ", "d").replace("Đ", "d");
        normalized = java.text.Normalizer.normalize(normalized, java.text.Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase();
    }

    /**
     * Helper method to check if any of the keywords match the given texts using Vietnamese normalization
     * @param keyword The search keyword (can contain multiple words separated by spaces)
     * @param texts Variable number of texts to search in
     * @return true if any keyword matches any text
     */
    private boolean matchesVietnameseKeyword(String keyword, String... texts) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String[] keywords = keyword.trim().split("\\s+");
        for (String kw : keywords) {
            String normalizedKw = normalizeVietnamese(kw);
            for (String text : texts) {
                String normalizedText = normalizeVietnamese(text != null ? text : "");
                if (normalizedText.contains(normalizedKw)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ApplicationStatusCheckData checkApplicationStatus(Long userId, Long recruitmentId) throws AppException {
        // Verify recruitment exists
        Recruitment recruitment = recruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // Check if user has already applied for this recruitment
        Optional<RecruitmentApplication> existingApp = applicationRepository.findByApplicant_IdAndRecruitment_Id(userId, recruitmentId);

        return ApplicationStatusCheckData.builder()
                .hasApplied(existingApp.isPresent())
                .build();
    }

}
