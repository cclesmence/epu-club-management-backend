package com.sep490.backendclubmanagement.service.report;

import com.sep490.backendclubmanagement.dto.request.CreateReportRequirementRequest;
import com.sep490.backendclubmanagement.dto.request.CreateReportRequest;
import com.sep490.backendclubmanagement.dto.request.ReportReviewRequest;
import com.sep490.backendclubmanagement.dto.request.SubmitReportRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateReportRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateReportRequirementRequest;
import com.sep490.backendclubmanagement.dto.response.OfficerReportRequirementResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.ReportDetailResponse;
import com.sep490.backendclubmanagement.dto.response.ReportListItemResponse;
import com.sep490.backendclubmanagement.dto.response.ReportRequirementResponse;
import com.sep490.backendclubmanagement.entity.*;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.club.ClubReportRequirement;
import com.sep490.backendclubmanagement.entity.event.Event;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.exception.ErrorCode;
import com.sep490.backendclubmanagement.exception.ForbiddenException;
import com.sep490.backendclubmanagement.exception.NotFoundException;
import com.sep490.backendclubmanagement.mapper.ReportMapper;
import com.sep490.backendclubmanagement.mapper.SubmissionReportRequirementMapper;
import com.sep490.backendclubmanagement.repository.ClubMemberShipRepository;
import com.sep490.backendclubmanagement.repository.ClubReportRequirementRepository;
import com.sep490.backendclubmanagement.repository.ClubRepository;
import com.sep490.backendclubmanagement.repository.EventRepository;
import com.sep490.backendclubmanagement.repository.ReportRepository;
import com.sep490.backendclubmanagement.repository.RoleMemberShipRepository;
import com.sep490.backendclubmanagement.repository.SemesterRepository;
import com.sep490.backendclubmanagement.repository.SubmissionReportRequirementRepository;
import com.sep490.backendclubmanagement.repository.TeamRepository;
import com.sep490.backendclubmanagement.repository.UserRepository;
import com.sep490.backendclubmanagement.service.role.RoleService;
import com.sep490.backendclubmanagement.service.file.CloudinaryService;
import com.sep490.backendclubmanagement.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportServiceInterface {

    private final ReportRepository reportRepository;
    private final ClubReportRequirementRepository clubReportRequirementRepository;
    private final SubmissionReportRequirementRepository submissionReportRequirementRepository;
    private final ClubRepository clubRepository;
    private final ClubMemberShipRepository clubMemberShipRepository;
    private final EventRepository eventRepository;
    private final RoleService roleService;
    private final ReportMapper reportMapper;
    private final SubmissionReportRequirementMapper submissionReportRequirementMapper;
    private final RoleMemberShipRepository roleMemberShipRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;


    // Maximum allowed file size for uploads: 20 MB
    private static final long MAX_FILE_SIZE_BYTES = 20L * 1024L * 1024L;

    // University-level report statuses for staff access
    private static final Set<ReportStatus> UNIVERSITY_LEVEL_STATUSES = EnumSet.of(
            ReportStatus.PENDING_UNIVERSITY,
            ReportStatus.APPROVED_UNIVERSITY,
            ReportStatus.REJECTED_UNIVERSITY,
            ReportStatus.RESUBMITTED_UNIVERSITY
    );

    /**
     * Check that the provided MultipartFile does not exceed the configured max file size.
     * Throws IllegalArgumentException if the file is too large or invalid.
     */
    private void checkFileSize(MultipartFile file) throws AppException{
        if (file == null || file.isEmpty()) return;
        long size = file.getSize();
        if (size <= 0) {
            throw new IllegalArgumentException("Uploaded file is empty or invalid");
        }
        if (size > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE, "Kích thước tập tin vượt quá giới hạn 20MB");
        }
    }

    /**
     * Get all reports with filters and pagination (for staff only)
     * Only returns reports with university-level status: PENDING_UNIVERSITY, APPROVED_UNIVERSITY, REJECTED_UNIVERSITY, RESUBMITTED_UNIVERSITY
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportListItemResponse> getAllReports(
            ReportStatus status, Long clubId, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId) {

        // Staff can only view university-level reports
        // If status is provided, validate it's a university-level status
        if (status != null && !UNIVERSITY_LEVEL_STATUSES.contains(status)) {
            throw new ForbiddenException(
                    "Staff can only view reports with status: PENDING_UNIVERSITY, APPROVED_UNIVERSITY, REJECTED_UNIVERSITY, or RESUBMITTED_UNIVERSITY"
            );
        }

        Page<Report> reportPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all reports without keyword filter
            reportPage = reportRepository.findAllWithFilters(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    null,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization - optimized with helper method
            List<Report> filteredList = reportPage.getContent().stream()
                    .filter(report -> matchesVietnameseKeyword(trimmedKeyword,
                            report.getReportTitle(),
                            report.getContent()))
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<Report> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            reportPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            reportPage = reportRepository.findAllWithFilters(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    keyword,
                    pageable
            );
        }

        // Filter to only include university-level reports - optimized with EnumSet
        List<Report> filteredReports = reportPage.getContent().stream()
                .filter(report -> UNIVERSITY_LEVEL_STATUSES.contains(report.getStatus()))
                .toList();

        // Map to response
        List<ReportListItemResponse> filteredContent = filteredReports.stream()
                .map(reportMapper::toListItem)
                .toList();

        // Create PageResponse with filtered content
        // Note: totalElements and totalPages reflect the filtered results
        return PageResponse.<ReportListItemResponse>builder()
                .content(filteredContent)
                .pageNumber(reportPage.getNumber() + 1)
                .pageSize(reportPage.getSize())
                .totalElements(filteredContent.size())
                .totalPages((int) Math.ceil((double) filteredContent.size() / reportPage.getSize()))
                .hasNext(reportPage.getNumber() < reportPage.getTotalPages() - 1 && filteredContent.size() == reportPage.getSize())
                .hasPrevious(reportPage.getNumber() > 0)
                .build();
    }

    /**
     * Get report detail by ID (for staff only)
     * Only allows viewing reports with university-level status
     */
    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getReportDetail(Long reportId, Long userId) {

        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + reportId));

        // Staff can only view university-level reports - optimized with EnumSet
        if (!UNIVERSITY_LEVEL_STATUSES.contains(report.getStatus())) {
            throw new ForbiddenException(
                    "Staff can only view reports with status: PENDING_UNIVERSITY, APPROVED_UNIVERSITY, REJECTED_UNIVERSITY, or RESUBMITTED_UNIVERSITY. " +
                    "Current status: " + report.getStatus()
            );
        }

        return reportMapper.toDetail(report);
    }

    /**
     * Review (approve/reject) a report (for staff only)
     * Updates ClubReportRequirement status instead of Report status
     */
    @Override
    @Transactional
    public void reviewReport(ReportReviewRequest request, Long userId) {

        Report report = reportRepository.findByIdWithRelations(request.getReportId())
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + request.getReportId()));

        // Only allow reviewing reports with status PENDING_UNIVERSITY, or RESUBMITTED_UNIVERSITY
        if (report.getStatus() != ReportStatus.PENDING_UNIVERSITY
                && report.getStatus() != ReportStatus.RESUBMITTED_UNIVERSITY) {
            throw new ForbiddenException(
                    "Only reports with status PENDING_UNIVERSITY, or RESUBMITTED_UNIVERSITY can be reviewed. " +
                    "Current status: " + report.getStatus()
            );
        }

        // Validate status and update report status accordingly
        // Staff only handles university-level reviews
        ReportStatus newReportStatus;
        
        if (request.getStatus() == ReportStatus.APPROVED_UNIVERSITY) {
            newReportStatus = ReportStatus.APPROVED_UNIVERSITY;
        } else if (request.getStatus() == ReportStatus.REJECTED_UNIVERSITY) {
            newReportStatus = ReportStatus.REJECTED_UNIVERSITY;
        } else {
            throw new ForbiddenException(
                    "Staff can only approve or reject reports. Status must be APPROVED_UNIVERSITY or REJECTED_UNIVERSITY. " +
                    "Current report status: " + report.getStatus()
            );
        }
        
        // Update report status
        report.setStatus(newReportStatus);

        // Get ClubReportRequirement from report to update note if needed
        if (report.getClubReportRequirement() != null && request.getReviewerFeedback() != null) {
            ClubReportRequirement clubReportRequirement = report.getClubReportRequirement();
            clubReportRequirementRepository.save(clubReportRequirement);
        }

        // Update report reviewed date and feedback (for tracking purposes)
        report.setReviewedDate(LocalDateTime.now());
        if (request.getReviewerFeedback() != null) {
            report.setReviewerFeedback(request.getReviewerFeedback());
        }
        
        // Handle mustResubmit field
        if (newReportStatus == ReportStatus.APPROVED_UNIVERSITY) {
            // When approving, set mustResubmit to false
            report.setMustResubmit(false);
        } else if (newReportStatus == ReportStatus.REJECTED_UNIVERSITY) {
            // When rejecting, use mustResubmit from request if provided, otherwise default to true
            if (request.getMustResubmit() != null) {
                report.setMustResubmit(request.getMustResubmit());
            } else {
                report.setMustResubmit(true);
            }
        }
        
        reportRepository.save(report);

        log.info("Staff {} has reviewed report {} with status {}", userId, request.getReportId(), newReportStatus);

        // Send notification to club officers and report creator
        try {
            if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getClub() != null) {
                Club club = report.getClubReportRequirement().getClub();
                Long reportClubId = club.getId();
                String reportTitle = report.getReportTitle() != null ? report.getReportTitle() : "Báo cáo";
                String actionUrl = "/myclub/" + reportClubId + "/reports";

                // Get recipients (club officers)
                List<Long> recipientIds = new ArrayList<>(getClubOfficersInCurrentSemester(reportClubId));

                // Add creator if not already in the list
                if (report.getCreatedBy() != null) {
                    Long creatorId = report.getCreatedBy().getId();
                    if (!recipientIds.contains(creatorId)) {
                        recipientIds.add(creatorId);
                    }
                }

                if (!recipientIds.isEmpty()) {
                    String title;
                    String message;
                    NotificationType notificationType;
                    NotificationPriority priority = NotificationPriority.NORMAL;

                    if (newReportStatus == ReportStatus.APPROVED_UNIVERSITY) {
                        title = "Báo cáo được nhà trường phê duyệt";
                        message = "Báo cáo \"" + reportTitle + "\" của CLB " + club.getClubName() + " đã được nhà trường phê duyệt.";
                        notificationType = NotificationType.REPORT_APPROVED;
                        priority = NotificationPriority.HIGH;
                    } else {
                        title = "Báo cáo bị nhà trường từ chối";
                        message = "Báo cáo \"" + reportTitle + "\" của CLB " + club.getClubName() + " đã bị nhà trường từ chối.";

                        if (request.getReviewerFeedback() != null && !request.getReviewerFeedback().trim().isEmpty()) {
                            message += " Phản hồi: " + request.getReviewerFeedback();
                        }

                        if (report.isMustResubmit()) {
                            message += " Vui lòng chỉnh sửa và nộp lại.";
                        }

                        notificationType = NotificationType.REPORT_REJECTED;
                    }

                    notificationService.sendToUsers(
                            recipientIds,
                            userId,
                            title,
                            message,
                            notificationType,
                            priority,
                            actionUrl,
                            reportClubId,
                            null,
                            null,
                            null
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send staff review notification: {}", e.getMessage());
        }
    }

    /**
     * Create report requirement for multiple clubs (for staff only)
     */
    @Override
    @Transactional
    public ReportRequirementResponse createReportRequirement(CreateReportRequirementRequest request, MultipartFile file, Long userId) throws AppException {

        // Validate due date is not in the past
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must not be in the past");
        }

        // Validate file size before uploading
        if (file != null && !file.isEmpty()) {
            checkFileSize(file);
        }

        // Validate and get all clubs (batch load) - moved before event check for fail-fast
        List<Club> clubs = clubRepository.findAllById(request.getClubIds());
        if (clubs.size() != request.getClubIds().size()) {
            throw new NotFoundException("One or more clubs not found");
        }

        // Validate and get event if provided
        Event event = null;
        if (request.getEventId() != null) {
            event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new NotFoundException("Event not found"));
        }

        // Get current user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Upload file if provided
        String templateUrl = request.getTemplateUrl();
        if (file != null && !file.isEmpty()) {
            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadFile(file, "report_requirements");
                templateUrl = uploadResult.url();
                log.info("Uploaded template file for report requirement: {}", templateUrl);
            } catch (Exception e) {
                log.error("Failed to upload template file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload template file: " + e.getMessage(), e);
            }
        }

        // Create SubmissionReportRequirement
        SubmissionReportRequirement submissionRequirement = SubmissionReportRequirement.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .reportType(request.getReportType())
                .templateUrl(templateUrl)
                .event(event)
                .createdBy(user)
                .build();

        SubmissionReportRequirement savedSubmissionRequirement = submissionReportRequirementRepository.save(submissionRequirement);

        // Create ClubReportRequirement for each club - batch create to reduce DB round trips
        List<ClubReportRequirement> clubRequirements = new ArrayList<>(clubs.size());
        for (Club club : clubs) {
            ClubReportRequirement clubRequirement = ClubReportRequirement.builder()
                    .club(club)
                    .submissionReportRequirement(savedSubmissionRequirement)
                    .build();
            clubRequirements.add(clubRequirement);
        }

        // Batch save all club requirements
        List<ClubReportRequirement> savedClubRequirements = clubReportRequirementRepository.saveAll(clubRequirements);

        // Build response infos
        List<ReportRequirementResponse.ClubRequirementInfo> clubRequirementInfos = new ArrayList<>(savedClubRequirements.size());
        for (ClubReportRequirement savedClubRequirement : savedClubRequirements) {
            Club club = savedClubRequirement.getClub();
            clubRequirementInfos.add(ReportRequirementResponse.ClubRequirementInfo.builder()
                    .id(savedClubRequirement.getId())
                    .clubId(club.getId())
                    .clubName(club.getClubName())
                    .clubCode(club.getClubCode())
                    .teamId(savedClubRequirement.getTeamId())
                    .build());
        }

        log.info("Staff {} has created report requirement {} for {} clubs", userId, savedSubmissionRequirement.getId(), clubs.size());

        // Map SubmissionReportRequirement to response using mapper
        ReportRequirementResponse response = submissionReportRequirementMapper.toDto(savedSubmissionRequirement);
        
        // Set clubRequirements (not mapped by mapper as it comes from ClubReportRequirement)
        response.setClubRequirements(clubRequirementInfos);

        // Send notification to club officers of affected clubs
        try {
            String requirementTitle = request.getTitle() != null ? request.getTitle() : "Yêu cầu báo cáo mới";

            // Fetch current semester once for all clubs
            Semester currentSemester = semesterRepository.findByIsCurrentTrue().orElse(null);

            if (currentSemester != null) {
                // Batch load all club officers for all clubs in ONE query
                List<Long> clubIds = clubs.stream().map(Club::getId).toList();
                List<Object[]> officerData = roleMemberShipRepository
                        .findClubOfficerUserIdsByClubIdsAndSemesterId(clubIds, currentSemester.getId());

                // Group officers by clubId: Map<ClubId, List<UserId>>
                Map<Long, List<Long>> clubOfficersMap = new HashMap<>();
                for (Object[] row : officerData) {
                    Long clubId = (Long) row[0];
                    Long userOfficerId = (Long) row[1];
                    clubOfficersMap.computeIfAbsent(clubId, k -> new ArrayList<>()).add(userOfficerId);
                }

                // Send notifications to all clubs sequentially
                for (Club club : clubs) {
                    try {
                        List<Long> officerIds = clubOfficersMap.getOrDefault(club.getId(), Collections.emptyList());
                        String actionUrl = "/myclub/" + club.getId() + "/reports";

                        if (!officerIds.isEmpty()) {
                            String title = "Yêu cầu báo cáo mới từ nhà trường";
                            String message = "CLB " + club.getClubName() + " có yêu cầu báo cáo mới: \"" + requirementTitle + "\"";

                            if (request.getDueDate() != null) {
                                message += ". Hạn nộp: " + request.getDueDate();
                            }

                            notificationService.sendToUsers(
                                    officerIds,
                                    userId,
                                    title,
                                    message,
                                    NotificationType.SYSTEM_ANNOUNCEMENT,
                                    NotificationPriority.HIGH,
                                    actionUrl,
                                    club.getId(),
                                    null,
                                    null,
                                    null
                            );
                        }
                    } catch (Exception e) {
                        log.error("Failed to send notification for club {}: {}", club.getId(), e.getMessage());
                    }
                }
            } else {
                log.warn("Current semester not found, skipping notifications");
            }
        } catch (Exception e) {
            log.error("Failed to send report requirement notification: {}", e.getMessage());
        }

        return response;
    }

    /**
     * Update report requirement basic information (for staff only)
     */
    @Override
    @Transactional
    public ReportRequirementResponse updateReportRequirement(Long requirementId, UpdateReportRequirementRequest request, MultipartFile file, Long userId) throws AppException {

        // Validate due date is not in the past
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date must not be in the past");
        }

        // Validate file size before uploading
        if (file != null && !file.isEmpty()) {
            checkFileSize(file);
        }

        // Get existing submission report requirement
        SubmissionReportRequirement submissionRequirement = submissionReportRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Report requirement not found with ID: " + requirementId));

        // Upload file if provided and get URL
        String templateUrl = request.getTemplateUrl();
        if (file != null && !file.isEmpty()) {
            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadFile(file, "report_requirements");
                templateUrl = uploadResult.url();
                log.info("Uploaded new template file for report requirement: {}", templateUrl);
            } catch (Exception e) {
                log.error("Failed to upload template file: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload template file: " + e.getMessage(), e);
            }
        }

        // Update fields
        submissionRequirement.setTitle(request.getTitle());
        submissionRequirement.setDescription(request.getDescription());
        if (request.getDueDate() != null) {
            submissionRequirement.setDueDate(request.getDueDate());
        }
        if (templateUrl != null) {
            submissionRequirement.setTemplateUrl(templateUrl);
        }

        SubmissionReportRequirement updatedSubmissionRequirement = submissionReportRequirementRepository.save(submissionRequirement);

        log.info("Staff {} has updated report requirement {}", userId, requirementId);

        // Get all club requirements for this submission requirement
        List<ClubReportRequirement> clubRequirements = clubReportRequirementRepository
                .findBySubmissionReportRequirementId(requirementId);

        List<ReportRequirementResponse.ClubRequirementInfo> clubRequirementInfos = clubRequirements.stream()
                .map(clubReq -> ReportRequirementResponse.ClubRequirementInfo.builder()
                        .id(clubReq.getId())
                        .clubId(clubReq.getClub().getId())
                        .clubName(clubReq.getClub().getClubName())
                        .clubCode(clubReq.getClub().getClubCode())
                        .teamId(clubReq.getTeamId())
                        .build())
                .collect(Collectors.toList());

        // Map SubmissionReportRequirement to response using mapper
        ReportRequirementResponse response = submissionReportRequirementMapper.toDto(updatedSubmissionRequirement);
        response.setClubRequirements(clubRequirementInfos);

        // Send notification to club officers of affected clubs about the update
        try {
            String requirementTitle = request.getTitle() != null ? request.getTitle() : "Yêu cầu báo cáo";

            // Fetch current semester once for all clubs
            Semester currentSemester = semesterRepository.findByIsCurrentTrue().orElse(null);

            if (currentSemester != null) {
                // Batch load all club officers for all clubs in ONE query
                List<Long> clubIds = clubRequirements.stream()
                        .map(crr -> crr.getClub().getId())
                        .distinct()
                        .toList();

                List<Object[]> officerData = roleMemberShipRepository
                        .findClubOfficerUserIdsByClubIdsAndSemesterId(clubIds, currentSemester.getId());

                // Group officers by clubId: Map<ClubId, List<UserId>>
                Map<Long, List<Long>> clubOfficersMap = new HashMap<>();
                for (Object[] row : officerData) {
                    Long clubId = (Long) row[0];
                    Long userOfficerId = (Long) row[1];
                    clubOfficersMap.computeIfAbsent(clubId, k -> new ArrayList<>()).add(userOfficerId);
                }

                // Process all club notifications sequentially
                for (ClubReportRequirement clubReq : clubRequirements) {
                    try {
                        Club club = clubReq.getClub();
                        List<Long> officerIds = clubOfficersMap.getOrDefault(club.getId(), Collections.emptyList());
                        String actionUrl = "/myclub/" + club.getId() + "/reports";

                        if (!officerIds.isEmpty()) {
                            String title = "Cập nhật yêu cầu báo cáo từ nhà trường";
                            String message = "CLB " + club.getClubName() + " - Yêu cầu báo cáo \"" + requirementTitle + "\" đã được cập nhật";

                            if (request.getDueDate() != null) {
                                message += ". Hạn nộp mới: " + request.getDueDate();
                            }

                            notificationService.sendToUsers(
                                    officerIds,
                                    userId,
                                    title,
                                    message,
                                    NotificationType.SYSTEM_ANNOUNCEMENT,
                                    NotificationPriority.HIGH,
                                    actionUrl,
                                    club.getId(),
                                    null,
                                    null,
                                    null
                            );
                        }
                    } catch (Exception e) {
                        log.error("Failed to send notification for club {}: {}", clubReq.getClub().getId(), e.getMessage());
                    }
                }
            } else {
                log.warn("Current semester not found, skipping notifications");
            }
        } catch (Exception e) {
            log.error("Failed to send report requirement update notification: {}", e.getMessage());
        }

        return response;
    }

    /**
     * Create a report with file upload (draft for team officer, can submit for club president)
     * If autoSubmit is true and user is club president, the report will be automatically submitted
     */
    @Override
    @Transactional
    public ReportDetailResponse createReport(CreateReportRequest request, MultipartFile file, Long userId) throws AppException {

        // Get current semester early
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new NotFoundException("Current semester not found"));


        // Validate club exists and is active
        Club club = clubRepository.findById(request.getClubId())
                .orElseThrow(() -> new NotFoundException("Club not found with ID: " + request.getClubId()));

        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Validate report requirement exists
        SubmissionReportRequirement reportRequirement = submissionReportRequirementRepository.findById(request.getReportRequirementId())
                .orElseThrow(() -> new NotFoundException("Report requirement not found with ID: " + request.getReportRequirementId()));

        // Find ClubReportRequirement for this club and submission requirement
        ClubReportRequirement clubReportRequirement = clubReportRequirementRepository
                .findByClubIdAndSubmissionReportRequirementId(request.getClubId(), request.getReportRequirementId())
                .orElseThrow(() -> new NotFoundException(
                        "ClubReportRequirement not found for clubId: " + request.getClubId() +
                                " and submissionReportRequirementId: " + request.getReportRequirementId()
                ));

        // Check if a report already exists for this ClubReportRequirement
        if (clubReportRequirement.getReport() != null) {
            throw new ForbiddenException("Báo cáo cho yêu cầu này đã tồn tại. Vui lòng chỉnh sửa báo cáo đã có.");
        }

        // Check deadline before creating report (no existing report, so pass null)
        validateDeadlineForAction(reportRequirement, null, "tạo báo cáo");

        // Validate file size before processing upload
        if (file != null && !file.isEmpty()) {
            checkFileSize(file);
        }

        // Get user (moved after validations for fail-fast)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        // Upload file if provided
        String fileUrl = request.getFileUrl(); // Use provided fileUrl if any
        if (file != null && !file.isEmpty()) {
            try {
                // Upload file to Cloudinary in club/reports folder
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadFile(file, "club/reports");
                fileUrl = uploadResult.url();
                log.info("Uploaded file for report: {}", fileUrl);
            } catch (Exception e) {
                log.error("Failed to upload file for report: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
            }
        }

        // Set the uploaded file URL to request
        request.setFileUrl(fileUrl);

        // Determine status based on role and autoSubmit flag
        ReportStatus status;
        boolean shouldAutoSubmit = false;

        boolean isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                userId, request.getClubId(), currentSemester.getId());
        if (isClubOfficer) {
            // Club president: if autoSubmit is true or null (default), create and submit directly
            // If autoSubmit is false, create as draft
            if (request.getAutoSubmit() == null || Boolean.TRUE.equals(request.getAutoSubmit())) {
                status = ReportStatus.PENDING_CLUB;
                shouldAutoSubmit = true;
            } else {
                status = ReportStatus.DRAFT;
            }
        } else {
            // Team officer: always create draft (ignore autoSubmit flag)
            status = ReportStatus.DRAFT;
        }

        // Create report with bidirectional relationship properly set
        Report report = Report.builder()
                .reportTitle(request.getReportTitle())
                .content(request.getContent())
                .fileUrl(request.getFileUrl())
                .status(status)
                .semester(currentSemester)
                .createdBy(user)
                .clubReportRequirement(clubReportRequirement)
                .build();

        if (status == ReportStatus.PENDING_CLUB) {
            report.setSubmittedDate(LocalDateTime.now());
        }

        // Set bidirectional relationship
        clubReportRequirement.setReport(report);

        // Save Report
        Report savedReport = reportRepository.save(report);

        log.info("User {} created report {} with status {} for club {} (autoSubmit: {})",
                userId, savedReport.getId(), status, request.getClubId(), shouldAutoSubmit);

        // Map to response using already loaded data (no need to query again)
        ReportDetailResponse reportDetailResponse = reportMapper.toDetail(savedReport);

        // Send notification if report was auto-submitted
        if (shouldAutoSubmit && status == ReportStatus.PENDING_CLUB) {
            try {
                String reportTitle = savedReport.getReportTitle() != null ? savedReport.getReportTitle() : "Báo cáo";
                String actionUrl = "/myclub/" + club.getId() + "/reports";

                // Get club officers
                List<Long> recipientIds = roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(club.getId(), currentSemester.getId())
                        .stream()
                        .filter(id -> !id.equals(userId))
                        .collect(Collectors.toList());

                if (!recipientIds.isEmpty()) {
                    User submitter = savedReport.getCreatedBy();
                    String submitterName = submitter != null ? submitter.getFullName() : "Người dùng";

                    String title = "Có báo cáo mới cần duyệt";
                    String message = submitterName + " đã nộp báo cáo \"" + reportTitle + "\" cần phê duyệt.";

                    notificationService.sendToUsers(
                            recipientIds,
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club.getId(),
                            null,
                            null,
                            null
                    );
                }
            } catch (Exception e) {
                log.error("Failed to send report submission notification: {}", e.getMessage());
            }
        }

        return reportDetailResponse;
    }


    /**
     * Update a draft report with file upload
     */
    @Override
    @Transactional
    public ReportDetailResponse updateReport(Long reportId, UpdateReportRequest request, MultipartFile file, Long userId) throws AppException{
        // Validate file size before processing upload
        if (file != null && !file.isEmpty()) {
            checkFileSize(file);
        }

        // Get report with relations (already optimized with JOIN FETCH)
        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + reportId));

        // Only allow updating draft reports, rejected reports (for resubmission), or pending club reports
        if (report.getStatus() != ReportStatus.DRAFT
                && report.getStatus() != ReportStatus.REJECTED_CLUB
                && report.getStatus() != ReportStatus.REJECTED_UNIVERSITY) {
            throw new ForbiddenException(
                    "Chỉ có thể cập nhật báo cáo ở trạng thái nháp (DRAFT), bị từ chối (REJECTED). " +
                            "Trạng thái hiện tại: " + report.getStatus()
            );
        }

        // Check if user is the creator (early return pattern)
        if (report.getCreatedBy() == null || !report.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("Bạn không có quyền cập nhật báo cáo này. Chỉ người tạo mới được chỉnh sửa.");
        }

        // Check if club is active
        if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getClub() != null) {
            Club club = report.getClubReportRequirement().getClub();
            if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
                throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
            }
        }

        // Get report requirement and check deadline
        if (report.getClubReportRequirement() != null &&
                report.getClubReportRequirement().getSubmissionReportRequirement() != null) {
            SubmissionReportRequirement reportRequirement = report.getClubReportRequirement()
                    .getSubmissionReportRequirement();
            validateDeadlineForAction(reportRequirement, report, "cập nhật báo cáo");
        }

        // Upload file if provided (only after all validations pass)
        String fileUrl = request.getFileUrl();
        if (file != null && !file.isEmpty()) {
            try {
                CloudinaryService.UploadResult uploadResult = cloudinaryService.uploadFile(file, "club/reports");
                fileUrl = uploadResult.url();
                log.info("Uploaded file for report update: {}", fileUrl);
            } catch (Exception e) {
                log.error("Failed to upload file for report update: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
            }
        }

        // Update report
        report.setReportTitle(request.getReportTitle());
        report.setContent(request.getContent());
        report.setFileUrl(fileUrl);

        Report updatedReport = reportRepository.save(report);

        log.info("User {} updated report {}", userId, reportId);

        return reportMapper.toDetail(updatedReport);
    }

    /**
     * Submit a draft report or resubmit a rejected report
     * Allowed for: club president OR team officer who is the creator
     */
    @Override
    @Transactional
    public ReportDetailResponse submitReport(SubmitReportRequest request, Long userId) throws AppException{
        // Get report with relations (already optimized with JOIN FETCH)
        Report report = reportRepository.findByIdWithRelations(request.getReportId())
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + request.getReportId()));

        // Only allow submitting draft reports or resubmitting rejected reports (early validation)
        if (report.getStatus() != ReportStatus.DRAFT
                && report.getStatus() != ReportStatus.REJECTED_CLUB
                && report.getStatus() != ReportStatus.REJECTED_UNIVERSITY) {
            throw new ForbiddenException(
                    "Chỉ có thể nộp báo cáo ở trạng thái nháp (DRAFT) hoặc bị từ chối (REJECTED). " +
                    "Trạng thái hiện tại: " + report.getStatus()
            );
        }

        // Check if club is active
        if (report.getClubReportRequirement() == null || report.getClubReportRequirement().getClub() == null) {
            throw new NotFoundException("Report must have an associated club");
        }

        Club club = report.getClubReportRequirement().getClub();
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        Long clubId = club.getId();

        // Get current semester once (avoid multiple queries)
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new NotFoundException("Current semester not found"));

        Long semesterId = currentSemester.getId();

        // Check permissions efficiently - single query for club officer
        boolean isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                userId, clubId, semesterId);

        // Get report requirement and check deadline
        if (report.getClubReportRequirement() != null &&
            report.getClubReportRequirement().getSubmissionReportRequirement() != null) {
            SubmissionReportRequirement reportRequirement = report.getClubReportRequirement()
                    .getSubmissionReportRequirement();
            validateDeadlineForAction(reportRequirement, report, "nộp báo cáo");
        }

        // Determine the appropriate status based on current status and user role
        ReportStatus currentStatus = report.getStatus();
        ReportStatus newStatus;

        if (currentStatus == ReportStatus.DRAFT) {
            // First submission: PENDING_CLUB
            newStatus = ReportStatus.PENDING_CLUB;
        } else if (currentStatus == ReportStatus.REJECTED_CLUB) {
            // Resubmission after club rejection: UPDATED_PENDING_CLUB
            newStatus = ReportStatus.UPDATED_PENDING_CLUB;
        } else if (currentStatus == ReportStatus.REJECTED_UNIVERSITY) {
            // Resubmission after university rejection:
            // - If club president: RESUBMITTED_UNIVERSITY (nộp lại lên trường)
            // - If team officer: UPDATED_PENDING_CLUB (nộp lại lên câu lạc bộ)
            if (isClubOfficer || currentStatus == ReportStatus.UPDATED_PENDING_CLUB) {
                newStatus = ReportStatus.RESUBMITTED_UNIVERSITY;
            } else {
                // Team officer resubmits to club level
                newStatus = ReportStatus.UPDATED_PENDING_CLUB;
            }
        } else {
            // Fallback (should not happen due to validation above)
            newStatus = ReportStatus.PENDING_CLUB;
        }

        // Reset reviewerFeedback when resubmitting to university (from REJECTED_UNIVERSITY to RESUBMITTED_UNIVERSITY)
        // Only reset when club president resubmits to university level
        if (currentStatus == ReportStatus.REJECTED_UNIVERSITY && newStatus == ReportStatus.RESUBMITTED_UNIVERSITY) {
            report.setReviewerFeedback(null);
        }
        
        // Update report status
        report.setStatus(newStatus);
        report.setSubmittedDate(LocalDateTime.now());

        // Reset mustResubmit when report is resubmitted
       // report.setMustResubmit(false);

        Report submittedReport = reportRepository.save(report);

        // Send notification based on new status
        try {
            String reportTitle = submittedReport.getReportTitle() != null ? submittedReport.getReportTitle() : "Báo cáo";
            String actionUrl = "/myclub/" + club.getId() + "/reports";

            if (newStatus == ReportStatus.PENDING_CLUB || newStatus == ReportStatus.UPDATED_PENDING_CLUB) {
                // Notify Club Officers when report is submitted to club level
                List<Long> recipientIds = roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(club.getId(), semesterId)
                        .stream()
                        .filter(id -> !id.equals(userId)) // Don't notify submitter
                        .collect(Collectors.toList());

                if (!recipientIds.isEmpty()) {
                    User submitter = submittedReport.getCreatedBy();
                    String submitterName = submitter != null ? submitter.getFullName() : "Người dùng";

                    String title = "Có báo cáo mới cần duyệt";
                    String message = submitterName + " đã nộp báo cáo \"" + reportTitle + "\" cần phê duyệt.";

                    notificationService.sendToUsers(
                            recipientIds,
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club.getId(),
                            null,
                            null,
                            null
                    );
                }
            } else if (newStatus == ReportStatus.RESUBMITTED_UNIVERSITY) {
                // Notify Staff when report is resubmitted to university level
                List<Long> staffIds = getStaffUsers();
                actionUrl = "/staff/reports";

                if (!staffIds.isEmpty()) {
                    String title = "Báo cáo được nộp lại từ CLB";
                    String message = "CLB " + club.getClubName() + " đã nộp lại báo cáo \"" + reportTitle + "\" cần xem xét.";

                    notificationService.sendToUsers(
                            staffIds,
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            club.getId(),
                            null,
                            null,
                            null
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send report submission notification: {}", e.getMessage());
        }

        return reportMapper.toDetail(submittedReport);
    }

    /**
     * Get all reports for a club (club president can see all)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportListItemResponse> getClubReports(
            Long clubId, ReportStatus status, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId) {
        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        Page<Report> reportPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all reports without keyword filter
            reportPage = reportRepository.findByClubIdWithFilter(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    null,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization - optimized with helper method
            List<Report> filteredList = reportPage.getContent().stream()
                    .filter(report -> matchesVietnameseKeyword(trimmedKeyword,
                            report.getReportTitle(),
                            report.getContent()))
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<Report> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            reportPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            reportPage = reportRepository.findByClubIdWithFilter(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    keyword,
                    pageable
            );
        }

        List<ReportListItemResponse> content = reportPage.getContent().stream()
                .map(reportMapper::toListItem)
                .toList();

        return PageResponse.<ReportListItemResponse>builder()
                .content(content)
                .pageNumber(reportPage.getNumber() + 1)
                .pageSize(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .hasNext(reportPage.hasNext())
                .hasPrevious(reportPage.hasPrevious())
                .build();

    }

    /**
     * Get my draft reports for a club
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportListItemResponse> getMyReports(
            Long clubId, ReportStatus status, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId) {
        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        Page<Report> reportPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all reports without keyword filter
            reportPage = reportRepository.findByClubIdAndUserIdWithFilter(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    null,
                    userId,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization - optimized with helper method
            List<Report> filteredList = reportPage.getContent().stream()
                    .filter(report -> matchesVietnameseKeyword(trimmedKeyword,
                            report.getReportTitle(),
                            report.getContent()))
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<Report> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            reportPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            reportPage = reportRepository.findByClubIdAndUserIdWithFilter(
                    status,
                    clubId,
                    semesterId,
                    reportType,
                    keyword,
                    userId,
                    pageable
            );
        }

        List<ReportListItemResponse> content = reportPage.getContent().stream()
                .map(reportMapper::toListItem)
                .toList();

        return PageResponse.<ReportListItemResponse>builder()
                .content(content)
                .pageNumber(reportPage.getNumber() + 1)
                .pageSize(reportPage.getSize())
                .totalElements(reportPage.getTotalElements())
                .totalPages(reportPage.getTotalPages())
                .hasNext(reportPage.hasNext())
                .hasPrevious(reportPage.hasPrevious())
                .build();

    }

    /**
     * Get all report requirements with filters and pagination (for staff only)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportRequirementResponse> getAllReportRequirements(
            ReportType reportType, Long clubId, String keyword, Pageable pageable, Long userId) {

        Page<SubmissionReportRequirement> requirementPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all requirements without keyword filter
            requirementPage = submissionReportRequirementRepository.findAllWithFilters(
                    reportType,
                    clubId,
                    null,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization - optimized with helper method
            List<SubmissionReportRequirement> filteredList = requirementPage.getContent().stream()
                    .filter(requirement -> matchesVietnameseKeyword(trimmedKeyword,
                            requirement.getTitle(),
                            requirement.getDescription()))
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<SubmissionReportRequirement> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            requirementPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            requirementPage = submissionReportRequirementRepository.findAllWithFilters(
                    reportType,
                    clubId,
                    keyword,
                    pageable
            );
        }

        // Map to response with club requirements
        // Fix N+1 queries: Load all club requirements in a single query
        List<Long> requirementIds = requirementPage.getContent().stream()
                .map(SubmissionReportRequirement::getId)
                .toList();

        // Batch load all club requirements for all submission requirements in ONE query
        Map<Long, List<ClubReportRequirement>> clubRequirementsMap = new HashMap<>();
        if (!requirementIds.isEmpty()) {
            List<ClubReportRequirement> allClubRequirements = clubReportRequirementRepository
                    .findBySubmissionReportRequirementIdIn(requirementIds);

            // Group by SubmissionReportRequirement ID
            clubRequirementsMap = allClubRequirements.stream()
                    .collect(Collectors.groupingBy(
                            crr -> crr.getSubmissionReportRequirement().getId()
                    ));
        }

        // Final map for use in lambda (must be effectively final)
        final Map<Long, List<ClubReportRequirement>> clubRequirementsMapFinal = clubRequirementsMap;

        Page<ReportRequirementResponse> responsePage = requirementPage.map(requirement -> {
            ReportRequirementResponse response = submissionReportRequirementMapper.toDto(requirement);
            
            // Get club requirements from pre-loaded map (NO additional queries!)
            List<ClubReportRequirement> clubRequirements = clubRequirementsMapFinal
                    .getOrDefault(requirement.getId(), Collections.emptyList());

            // Set club count instead of returning full club requirements list
            response.setClubCount(clubRequirements.size());
            // Set clubRequirements to null to avoid returning unnecessary data
            response.setClubRequirements(null);

            return response;
        });

        return PageResponse.<ReportRequirementResponse>builder()
                .content(responsePage.getContent())
                .pageNumber(responsePage.getNumber() + 1)
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();
    }

    /**
     * Get a single report requirement by ID (for staff only)
     */
    @Override
    @Transactional(readOnly = true)
    public ReportRequirementResponse getReportRequirementById(Long requirementId, Long userId) {

        // Get submission report requirement
        SubmissionReportRequirement requirement = submissionReportRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new NotFoundException("Report requirement not found with ID: " + requirementId));

        // Map to response using mapper
        ReportRequirementResponse response = submissionReportRequirementMapper.toDto(requirement);

        // Get club requirements count
        List<ClubReportRequirement> clubRequirements = clubReportRequirementRepository
                .findBySubmissionReportRequirementId(requirementId);

        // Set club count instead of returning full club requirements list
        response.setClubCount(clubRequirements.size());
        // Set clubRequirements to null to avoid returning unnecessary data
        response.setClubRequirements(null);

        return response;
    }

    /**
     * Get list of clubs that need to submit reports for a specific report requirement (for staff only)
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReportRequirementResponse.ClubRequirementInfo> getClubsByReportRequirement(
            Long requirementId, String keyword, Pageable pageable, Long userId) {

        // Validate submission report requirement exists
        if (!submissionReportRequirementRepository.existsById(requirementId)) {
            throw new NotFoundException("Report requirement not found with ID: " + requirementId);
        }

        // Get all club requirements for this submission requirement
        List<ClubReportRequirement> clubRequirements = clubReportRequirementRepository
                .findBySubmissionReportRequirementId(requirementId);

        // Apply keyword filter if provided
        List<ClubReportRequirement> filteredClubRequirements = clubRequirements;
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            filteredClubRequirements = clubRequirements.stream()
                    .filter(crr -> {
                        if (crr.getClub() == null) return false;
                        return matchesVietnameseKeyword(trimmedKeyword,
                                crr.getClub().getClubName(),
                                crr.getClub().getClubCode());
                    })
                    .toList();
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredClubRequirements.size());
        List<ClubReportRequirement> paginatedList = start >= filteredClubRequirements.size() ?
                Collections.emptyList() : filteredClubRequirements.subList(start, end);

        // Map to response
        List<ReportRequirementResponse.ClubRequirementInfo> content = paginatedList.stream()
                .map(crr -> {
                    // Get status from report if exists, otherwise null
                    String statusStr = null;
                    ReportRequirementResponse.ReportInfo reportInfo = null;
                    if (crr.getReport() != null) {
                        Report report = crr.getReport();
                        if (report.getStatus() != null) {
                            statusStr = report.getStatus().name();
                        }
                    }
                    return ReportRequirementResponse.ClubRequirementInfo.builder()
                            .id(crr.getId())
                            .clubId(crr.getClub().getId())
                            .clubName(crr.getClub().getClubName())
                            .clubCode(crr.getClub().getClubCode())
                            .status(statusStr)
                            .teamId(crr.getTeamId())
                            .report(reportInfo)
                            .build();
                })
                .toList();

        // Build page response
        int totalElements = filteredClubRequirements.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        return PageResponse.<ReportRequirementResponse.ClubRequirementInfo>builder()
                .content(content)
                .pageNumber(pageable.getPageNumber() + 1)
                .pageSize(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(pageable.getPageNumber() < totalPages - 1)
                .hasPrevious(pageable.getPageNumber() > 0)
                .build();
    }

    /**
     * Get report of a specific club for a specific report requirement (for staff only)
     * Only returns reports with university-level status
     */
    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getClubReportByRequirement(Long requirementId, Long clubId, Long userId) {

        // Validate submission report requirement exists - optimized: only check existence
        if (!submissionReportRequirementRepository.existsById(requirementId)) {
            throw new NotFoundException("Report requirement not found with ID: " + requirementId);
        }

        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        // Find report by clubId and requirementId
        Optional<Report> reportOptional = reportRepository.findByClubIdAndReportRequirementId(clubId, requirementId);

        // Return null if report doesn't exist (club hasn't submitted report yet)
        if (reportOptional.isEmpty()) {
            return null;
        }

        Report report = reportOptional.get();
        
        // Staff can only view university-level reports - optimized with EnumSet
        if (!UNIVERSITY_LEVEL_STATUSES.contains(report.getStatus())) {
            // Return null if report is not at university level (staff cannot view club-level reports)
            return null;
        }

        return reportMapper.toDetail(report);
    }

    /**
     * Get all report requirements for a club with filters and pagination (for CLUB_OFFICER or TEAM_OFFICER)
     * Optimized version that returns only essential fields
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OfficerReportRequirementResponse> getClubReportRequirementsForOfficerWithFilters(
            Long clubId, String status, Long semesterId, String keyword, Long teamId,
            Pageable pageable, Long userId) {
        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElse(null);

        // Check if user is CLUB_OFFICER or TEAM_OFFICER (from club_roles table), TREASURER in current semester
        Long userTeamId = null; // Team ID of team officer

        if (currentSemester != null) {
            // Check if user is club officer (not team officer)
            boolean isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                    userId, clubId, currentSemester.getId());

            if (!isClubOfficer) {
                // User is team officer, get their team ID
                userTeamId = roleMemberShipRepository.findTeamIdByUserIdAndClubIdAndSemesterId(
                        userId, clubId, currentSemester.getId()).orElse(null);
            }
        }

        // Determine filterTeamId:
        // - If user is team officer, always use their teamId (ignore teamId param for security)
        // - If user is club officer, use teamId param if provided, otherwise null (show all)
        Long filterTeamId = userTeamId != null ? userTeamId : teamId;

        // Parse status filter
        Boolean filterUnsubmitted = null;
        Boolean filterOverdue = null;
        ReportStatus reportStatus = null;

        if (status != null && !status.isEmpty()) {
            String statusStr = status.toUpperCase();
            if ("UNSUBMITTED".equals(statusStr)) {
                filterUnsubmitted = true;
            } else if ("OVERDUE".equals(statusStr)) {
                filterOverdue = true;
            } else {
                // Try to parse as ReportStatus enum
                try {
                    reportStatus = ReportStatus.valueOf(statusStr);
                } catch (IllegalArgumentException e) {
                    // Invalid status, ignore filter
                }
            }
        }

        // Get current date for overdue filter
        LocalDateTime currentDate = LocalDateTime.now();

        // Query with filters
        Page<ClubReportRequirement> requirementPage;

        // If keyword is provided, use client-side filtering with Vietnamese normalization
        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            // Get all club report requirements without keyword filter
            requirementPage = clubReportRequirementRepository.findByClubIdWithFilters(
                    clubId,
                    null,
                    filterUnsubmitted,
                    filterOverdue,
                    reportStatus,
                    semesterId,
                    filterTeamId,
                    currentDate,
                    PageRequest.of(0, Integer.MAX_VALUE)
            );

            // Filter using Vietnamese normalization on the submission requirement's title and description - optimized
            List<ClubReportRequirement> filteredList = requirementPage.getContent().stream()
                    .filter(crr -> {
                        SubmissionReportRequirement srr = crr.getSubmissionReportRequirement();
                        if (srr == null) return false;
                        return matchesVietnameseKeyword(trimmedKeyword,
                                srr.getTitle(),
                                srr.getDescription());
                    })
                    .toList();

            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredList.size());
            List<ClubReportRequirement> paginatedList = start >= filteredList.size() ?
                    Collections.emptyList() : filteredList.subList(start, end);
            requirementPage = new PageImpl<>(paginatedList, pageable, filteredList.size());
        } else {
            requirementPage = clubReportRequirementRepository.findByClubIdWithFilters(
                    clubId,
                    keyword,
                    filterUnsubmitted,
                    filterOverdue,
                    reportStatus,
                    semesterId,
                    filterTeamId,
                    currentDate,
                    pageable
            );
        }

        // Map to optimized response - only include essential fields
        Page<OfficerReportRequirementResponse> responsePage = requirementPage.map(crr -> {
            SubmissionReportRequirement srr = crr.getSubmissionReportRequirement();

            // Build simplified club requirement with report info (only id, status, mustResubmit)
            OfficerReportRequirementResponse.SimplifiedReport reportInfo = null;
            if (crr.getReport() != null) {
                Report report = crr.getReport();
                Long reportCreatedById = report.getCreatedBy() != null ? report.getCreatedBy().getId() : null;
                reportInfo = OfficerReportRequirementResponse.SimplifiedReport.builder()
                        .id(report.getId())
                        .status(report.getStatus() != null ? report.getStatus().name() : null)
                        .mustResubmit(report.isMustResubmit())
                        .createdBy(reportCreatedById)
                        .createdByUserName(report.getCreatedBy() != null ? report.getCreatedBy().getFullName() : null)
                        .build();
            }
            
            // Use the provided clubId (all crr belong to this club) to avoid extra lazy loads
            OfficerReportRequirementResponse.SimplifiedClubRequirement clubRequirement =
                    OfficerReportRequirementResponse.SimplifiedClubRequirement.builder()
                            .id(crr.getId())
                            .clubId(clubId)
                            .teamId(crr.getTeamId())
                            .report(reportInfo)
                            .build();

            // Build main response with only essential requirement fields
            return OfficerReportRequirementResponse.builder()
                    .id(srr.getId())
                    .title(srr.getTitle())
                    .description(srr.getDescription())
                    .dueDate(srr.getDueDate())
                    .reportType(srr.getReportType())
                    .templateUrl(srr.getTemplateUrl())
                    .createdAt(srr.getCreatedAt())
                    .updatedAt(srr.getUpdatedAt())
                    .createdByName(srr.getCreatedBy() != null ? srr.getCreatedBy().getFullName() : null)
                    .clubRequirement(clubRequirement)
                    .build();
        });

        return PageResponse.<OfficerReportRequirementResponse>builder()
                .content(responsePage.getContent())
                .pageNumber(responsePage.getNumber() + 1)
                .pageSize(responsePage.getSize())
                .totalElements(responsePage.getTotalElements())
                .totalPages(responsePage.getTotalPages())
                .hasNext(responsePage.hasNext())
                .hasPrevious(responsePage.hasPrevious())
                .build();
    }

    /**
     * Get report of a specific club for a specific report requirement (for CLUB_OFFICER or TEAM_OFFICER)
     */
    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getClubReportByRequirementForOfficer(Long requirementId, Long clubId, Long userId) {
        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        // Validate submission report requirement exists - optimized: only check existence
        if (!submissionReportRequirementRepository.existsById(requirementId)) {
            throw new NotFoundException("Report requirement not found with ID: " + requirementId);
        }

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElse(null);

        // Check if user is CLUB_OFFICER or TEAM_OFFICER (from club_roles table) in current semester
        boolean isClubOfficerOrTeamOfficerOrTreasurer = false;

        if (currentSemester != null) {
            isClubOfficerOrTeamOfficerOrTreasurer = roleMemberShipRepository.isClubOfficerOrTeamOfficerOrTreasurerInCurrentSemester(
                    userId, clubId, currentSemester.getId());
        }

        if (!isClubOfficerOrTeamOfficerOrTreasurer) {
            throw new ForbiddenException(
                    "Chỉ cán bộ câu lạc bộ (CLUB_OFFICER) hoặc cán bộ ban (TEAM_OFFICER), Thủ quỹ " +
                    "trong kỳ hiện tại và đang hoạt động mới có quyền xem chi tiết báo cáo."
            );
        }

        // Find report by clubId and requirementId
        Optional<Report> reportOptional = reportRepository.findByClubIdAndReportRequirementId(clubId, requirementId);

        // Return null if report doesn't exist (club hasn't submitted report yet)
        if (reportOptional.isEmpty()) {
            return null;
        }

        Report report = reportOptional.get();
        return reportMapper.toDetail(report);
    }

    /**
     * Delete a draft report (only creator or team officer can delete their own draft)
     */
    @Override
    @Transactional
    public void deleteReport(Long reportId, Long userId) throws AppException {
        // Get report with relations
        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + reportId));

        // Check if club is active
        if (report.getClubReportRequirement() != null && report.getClubReportRequirement().getClub() != null) {
            Club club = report.getClubReportRequirement().getClub();
            if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
                throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
            }
        }

        // Only allow deleting draft reports
        if (report.getStatus() != ReportStatus.DRAFT) {
            throw new ForbiddenException(
                    "Chỉ có thể xóa báo cáo ở trạng thái nháp (DRAFT). " +
                    "Trạng thái hiện tại: " + report.getStatus()
            );
        }

        // Check if user is the creator
        boolean isCreator = report.getCreatedBy() != null && report.getCreatedBy().getId().equals(userId);
        
        if (!isCreator) {
            throw new ForbiddenException("Bạn không có quyền xóa báo cáo này. Chỉ người tạo mới được xóa.");
        }

        // Clear bidirectional association to avoid re-persisting or FK issues
        if (report.getClubReportRequirement() != null) {
            ClubReportRequirement clubReq = report.getClubReportRequirement();
            // break the in-memory link from ClubReportRequirement -> Report
            clubReq.setReport(null);
            // persist the change so JPA is aware before deleting the report
            clubReportRequirementRepository.save(clubReq);
        }

        // Delete the report
        reportRepository.delete(report);
        // Ensure changes are flushed within this transaction
        reportRepository.flush();

        log.info("User {} deleted draft report {}", userId, reportId);
    }

    /**
     * Get report detail by report ID for (CLUB_OFFICER or TEAM_OFFICER)
     */
    @Override
    @Transactional(readOnly = true)
    public ReportDetailResponse getClubReportDetail(Long reportId, Long clubId, Long userId) {
        // Validate club exists - optimized: only check existence
        if (!clubRepository.existsById(clubId)) {
            throw new NotFoundException("Club not found with ID: " + clubId);
        }

        // Get report with relations
        Report report = reportRepository.findByIdWithRelations(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + reportId));

        // Verify report belongs to the club
        if (report.getClubReportRequirement() == null || 
            !report.getClubReportRequirement().getClub().getId().equals(clubId)) {
            throw new ForbiddenException("Báo cáo không thuộc về câu lạc bộ này");
        }

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElse(null);

        boolean isClubOfficer = false;

        if (currentSemester != null) {
            isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                    userId, clubId, currentSemester.getId());
        }

        // If user is team officer, treasurer only allow viewing their own reports
        if (!isClubOfficer) {
            if (report.getCreatedBy() == null || !report.getCreatedBy().getId().equals(userId)) {
                throw new ForbiddenException("Bạn chỉ có thể xem báo cáo do chính bạn tạo");
            }
        }

        return reportMapper.toDetail(report);
    }

    /**
     * Review (approve/reject) a report at club level (for club president only)
     * Approve: PENDING_CLUB -> PENDING_UNIVERSITY
     * Reject: PENDING_CLUB -> REJECTED_CLUB
     */
    @Override
    @Transactional
    public ReportDetailResponse reviewReportByClub(ReportReviewRequest request, Long userId) throws AppException {
        // Get report with relations
        Report report = reportRepository.findByIdWithRelations(request.getReportId())
                .orElseThrow(() -> new NotFoundException("Report not found with ID: " + request.getReportId()));

        // Check if club is active
        if (report.getClubReportRequirement() == null || report.getClubReportRequirement().getClub() == null) {
            throw new NotFoundException("Report must have an associated club");
        }

        Club club = report.getClubReportRequirement().getClub();
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Only allow reviewing reports with status PENDING_CLUB or UPDATED_PENDING_CLUB
        if (report.getStatus() != ReportStatus.PENDING_CLUB
                && report.getStatus() != ReportStatus.UPDATED_PENDING_CLUB
                && report.getStatus() != ReportStatus.DRAFT) {
            throw new ForbiddenException(
                    "Chỉ có thể duyệt/từ chối báo cáo ở trạng thái chờ CLB phê duyệt (PENDING_CLUB,UPDATED_PENDING_CLUB,DRAFT). " +
                    "Trạng thái hiện tại: " + report.getStatus()
            );
        }

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElseThrow(() -> new NotFoundException("Current semester not found"));

        // Check if user is club president in current semester and active
        Long clubId = club.getId();
        boolean isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                userId, clubId, currentSemester.getId());

        if (!isClubOfficer) {
            throw new ForbiddenException(
                    "Chỉ chủ nhiệm câu lạc bộ (club president) trong kỳ hiện tại và đang hoạt động " +
                    "mới có quyền duyệt/từ chối báo cáo ở cấp CLB."
            );
        }

        // Check if user is the creator
        boolean isCreator = report.getCreatedBy() != null && report.getCreatedBy().getId().equals(userId);

        if (report.getStatus() == ReportStatus.DRAFT && !isCreator) {
            throw new ForbiddenException(
                    "Người dùng không phải là người đã tạo bản nháp báo cáo này. "
            );
        }

        // Get report requirement and check deadline (only if not mustResubmit)
        if (report.getClubReportRequirement() != null &&
            report.getClubReportRequirement().getSubmissionReportRequirement() != null) {
            SubmissionReportRequirement reportRequirement = report.getClubReportRequirement()
                    .getSubmissionReportRequirement();
            validateDeadlineForAction(reportRequirement, report, "đánh giá báo cáo");
        }



        // Update report status
        report.setStatus(request.getStatus());
        // DO NOT set reviewedDate for club-level reviews
        // Only staff reviews should have reviewedDate
        
        // Handle reviewerFeedback based on action
        if (request.getStatus() == ReportStatus.PENDING_UNIVERSITY) {
            // When approving and submitting to university, reset reviewerFeedback to null
            // This ensures that when a report is submitted to university level, any previous feedback is cleared
            report.setReviewerFeedback(null);
            // Set submittedDate when submitting to university (if not already set)
            if (report.getSubmittedDate() == null) {
                report.setSubmittedDate(LocalDateTime.now());
            }
        } else if (request.getStatus() == ReportStatus.REJECTED_CLUB) {
            // When rejecting, set feedback if provided
            if (request.getReviewerFeedback() != null && !request.getReviewerFeedback().trim().isEmpty()) {
                report.setReviewerFeedback(request.getReviewerFeedback());
            }
        }

        Report reviewedReport = reportRepository.save(report);

        log.info("Club president {} reviewed report {} with status {}", userId, request.getReportId(), request.getStatus());

        // Send notification based on review result
        try {
            Long reviewClubId = club.getId();
            String reportTitle = reviewedReport.getReportTitle() != null ? reviewedReport.getReportTitle() : "Báo cáo";

            if (request.getStatus() == ReportStatus.PENDING_UNIVERSITY) {
                // Notify Staff
                List<Long> staffIds = getStaffUsers();

                if (!staffIds.isEmpty()) {
                    String actionUrl = "/staff/reports";
                    String title = "Có báo cáo mới cần duyệt";
                    String message = "CLB " + club.getClubName() + " đã gửi báo cáo \"" + reportTitle + "\" cần phê duyệt.";

                    notificationService.sendToUsers(
                            staffIds,
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_SUBMITTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            reviewClubId,
                            null,
                            null,
                            null
                    );
                }

                // Also notify the creator that report was approved by club
                if (reviewedReport.getCreatedBy() != null && !reviewedReport.getCreatedBy().getId().equals(userId)) {
                    Long creatorId = reviewedReport.getCreatedBy().getId();
                    String actionUrl = "/myclub/" + club.getId() + "/reports";
                    String title = "Báo cáo được CLB phê duyệt";
                    String message = "Báo cáo \"" + reportTitle + "\" của bạn đã được CLB phê duyệt và gửi lên nhà trường.";

                    notificationService.sendToUsers(
                            List.of(creatorId),
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_APPROVED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            reviewClubId,
                            null,
                            null,
                            null
                    );
                }
            } else if (request.getStatus() == ReportStatus.REJECTED_CLUB) {
                // Rejected by club, notify creator
                if (reviewedReport.getCreatedBy() != null) {
                    Long creatorId = reviewedReport.getCreatedBy().getId();
                    String actionUrl = "/myclub/" + club.getId() + "/reports";
                    String title = "Báo cáo bị từ chối";
                    String message = "Báo cáo \"" + reportTitle + "\" của bạn đã bị CLB từ chối.";

                    if (request.getReviewerFeedback() != null && !request.getReviewerFeedback().trim().isEmpty()) {
                        message += " Phản hồi: " + request.getReviewerFeedback();
                    }

                    notificationService.sendToUsers(
                            List.of(creatorId),
                            userId,
                            title,
                            message,
                            NotificationType.REPORT_REJECTED,
                            NotificationPriority.NORMAL,
                            actionUrl,
                            reviewClubId,
                            null,
                            null,
                            null
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send report review notification: {}", e.getMessage());
        }

        return reportMapper.toDetail(reviewedReport);
    }

    /**
     * Assign a team to a report requirement (for CLUB_OFFICER only)
     */
    @Override
    @Transactional
    public ReportRequirementResponse assignTeamToReportRequirement(
            Long clubReportRequirementId,
            Long teamId,
            Long clubId,
            Long userId
    ) throws AppException {
        // Validate club exists
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new NotFoundException("Club not found with ID: " + clubId));

        // Check if club is active (only active clubs can assign teams)
        if (!"ACTIVE".equalsIgnoreCase(club.getStatus())) {
            throw new AppException(ErrorCode.CLUB_NOT_ACTIVE);
        }

        // Get current semester
        Semester currentSemester = semesterRepository.findCurrentSemester()
                .orElse(null);

        // Check if user is CLUB_OFFICER (club president) in current semester
        boolean isClubOfficer = false;
        if (currentSemester != null) {
            isClubOfficer = roleMemberShipRepository.isClubOfficerInCurrentSemester(
                    userId, clubId, currentSemester.getId());
        }

        if (!isClubOfficer) {
            throw new ForbiddenException(
                    "Chỉ chủ nhiệm câu lạc bộ (CLUB_OFFICER) trong kỳ hiện tại và đang hoạt động " +
                    "mới có quyền gán team cho yêu cầu báo cáo."
            );
        }

        // Validate ClubReportRequirement exists and belongs to the club
        ClubReportRequirement clubReportRequirement = clubReportRequirementRepository.findById(clubReportRequirementId)
                .orElseThrow(() -> new NotFoundException(
                        "Club Report Requirement not found with ID: " + clubReportRequirementId));

        if (!clubReportRequirement.getClub().getId().equals(clubId)) {
            throw new ForbiddenException(
                    "Club Report Requirement does not belong to the specified club");
        }

        // Validate team exists and belongs to the club
        Team team = teamRepository.findByIdAndClubId(teamId, clubId)
                .orElseThrow(() -> new NotFoundException(
                        "Team not found with ID: " + teamId + " in club: " + clubId));

        // Get submission report requirement and check deadline
        if (clubReportRequirement.getSubmissionReportRequirement() != null) {
            SubmissionReportRequirement reportRequirement = clubReportRequirement.getSubmissionReportRequirement();
            validateDeadlineForAction(reportRequirement, clubReportRequirement.getReport(), "gán phòng ban cho báo cáo");
        }

        // Check if a report already exists for this requirement
        if (clubReportRequirement.getReport() != null) {
            throw new ForbiddenException(
                    "Không thể gán team cho yêu cầu báo cáo đã có báo cáo được tạo. " +
                    "Vui lòng xóa báo cáo trước khi gán lại team.");
        }

        // Update teamId
        clubReportRequirement.setTeamId(teamId);
        ClubReportRequirement savedClubReportRequirement = clubReportRequirementRepository.save(clubReportRequirement);

        log.info("Club officer {} assigned team {} to report requirement {} for club {}",
                userId, teamId, clubReportRequirementId, clubId);

        // Send notification to team members about the new assignment
        try {
            // Get team members
            List<Long> teamMemberIds = getTeamOfficersOrTreasurerInCurrentSemester(teamId, clubId);

            if (!teamMemberIds.isEmpty()) {
                SubmissionReportRequirement reportRequirement = savedClubReportRequirement.getSubmissionReportRequirement();
                String requirementTitle = reportRequirement != null ? reportRequirement.getTitle() : "Yêu cầu báo cáo";
                String actionUrl = "/myclub/" + club.getId() + "/reports";

                String title = "Ban của bạn được phân công báo cáo mới";
                String message = "Ban " + team.getTeamName() + " đã được phân công chịu trách nhiệm cho yêu cầu báo cáo: \"" + requirementTitle + "\"";

                if (reportRequirement != null && reportRequirement.getDueDate() != null) {
                    message += ". Hạn nộp: " + reportRequirement.getDueDate();
                }

                notificationService.sendToUsers(
                        teamMemberIds,
                        userId,
                        title,
                        message,
                        NotificationType.TEAM_ASSIGNMENT,
                        NotificationPriority.HIGH,
                        actionUrl,
                        clubId,
                        null,
                        teamId,
                        null
                );
            }
        } catch (Exception e) {
            log.error("Failed to send team assignment notification: {}", e.getMessage());
        }

        // Build response
        ReportRequirementResponse response = submissionReportRequirementMapper.toDto(
                savedClubReportRequirement.getSubmissionReportRequirement());

        // Get status from report if exists, otherwise null
        String statusStr = null;
        if (savedClubReportRequirement.getReport() != null && 
            savedClubReportRequirement.getReport().getStatus() != null) {
            statusStr = savedClubReportRequirement.getReport().getStatus().name();
        }

        // Build club requirement info
        ReportRequirementResponse.ClubRequirementInfo clubRequirementInfo = 
                ReportRequirementResponse.ClubRequirementInfo.builder()
                        .id(savedClubReportRequirement.getId())
                        .clubId(savedClubReportRequirement.getClub().getId())
                        .clubName(savedClubReportRequirement.getClub().getClubName())
                        .clubCode(savedClubReportRequirement.getClub().getClubCode())
                        .status(statusStr)
                        .teamId(savedClubReportRequirement.getTeamId())
                        .build();

        response.setClubRequirements(List.of(clubRequirementInfo));
        return response;
    }

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

        return roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(
                clubId, currentSemester.getId());
    }

    /**
     * Get list of Club Officer user IDs for a specific club and semester (overloaded for performance)
     * @param clubId Club ID
     * @param semesterId Semester ID (if already known, avoids extra query)
     * @return List of user IDs who are Club Officers
     */
    private List<Long> getClubOfficersInCurrentSemester(Long clubId, Long semesterId) {
        if (semesterId == null) {
            return getClubOfficersInCurrentSemester(clubId);
        }

        return roleMemberShipRepository.findClubOfficerUserIdsByClubIdAndSemesterId(
                clubId, semesterId);
    }

    /**
     * Get list of team officer or treasurer user IDs in current semester
     * @param teamId Team ID
     * @param clubId Club ID (kept for signature compatibility)
     * @return List of user IDs who are team officers or treasurers
     */
    private List<Long> getTeamOfficersOrTreasurerInCurrentSemester(Long teamId, Long clubId) {
        Semester currentSemester = semesterRepository.findByIsCurrentTrue()
                .orElse(null);

        if (currentSemester == null) {
            return Collections.emptyList();
        }

        // Use existing repository method (keeps original call-style used in this file)
        return roleMemberShipRepository.findTeamOfficerOrTreasurerUserIdsByClubIdAndSemesterId(
                teamId, currentSemester.getId());
    }

    /**
     * Get list of Staff user IDs
     * @return List of user IDs who are Staff
     */
    private List<Long> getStaffUsers() {
        List<User> staffUsers = userRepository.findBySystemRole_RoleNameIgnoreCase("STAFF");
        return staffUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to check if a report requirement is past due date
     * Returns true if the due date has passed and report is not in mustResubmit mode
     */
    private void validateDeadlineForAction(SubmissionReportRequirement reportRequirement, Report report, String action) {
        // Skip deadline check if report exists and mustResubmit is true
        if (report != null && report.isMustResubmit()) {
            return;
        }

        LocalDateTime dueDate = reportRequirement.getDueDate();
        if (dueDate != null && LocalDateTime.now().isAfter(dueDate)) {
            throw new ForbiddenException(
                    "Không thể " + action + " vì yêu cầu báo cáo đã quá hạn (" + dueDate + "). " +
                    "Vui lòng liên hệ nhà trường để được hỗ trợ."
            );
        }
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
}

