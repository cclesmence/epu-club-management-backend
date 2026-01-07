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
import com.sep490.backendclubmanagement.entity.ReportStatus;
import com.sep490.backendclubmanagement.entity.ReportType;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for Report Service
 */
public interface ReportServiceInterface {

    /**
     * Get all reports with filters and pagination (for staff only)
     * @param status Report status filter
     * @param clubId Club ID filter
     * @param semesterId Semester ID filter
     * @param reportType Report type filter
     * @param keyword Keyword for searching
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return Page response containing list of reports
     */
    PageResponse<ReportListItemResponse> getAllReports(
            ReportStatus status, Long clubId, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId);

    /**
     * Get report detail by ID (for staff only)
     * @param reportId Report ID
     * @param userId Current user ID
     * @return Report detail response
     */
    ReportDetailResponse getReportDetail(Long reportId, Long userId);

    /**
     * Review (approve/reject) a report (for staff only)
     * @param request Review request containing reportId, status (APPROVED/REJECTED), and optional feedback
     * @param userId Current user ID
     */
    void reviewReport(ReportReviewRequest request, Long userId);

    /**
     * Create report requirement for multiple clubs (for staff only)
     * @param request Create request containing requirement details and list of club IDs
     * @param file Optional template file to upload
     * @param userId Current user ID
     * @return Created report requirement response with club requirements
     */
    ReportRequirementResponse createReportRequirement(CreateReportRequirementRequest request, MultipartFile file, Long userId) throws  AppException;

    /**
     * Update report requirement basic information (for staff only)
     * @param requirementId Submission report requirement ID
     * @param request Update request containing updated title, description, dueDate, and templateUrl
     * @param file Optional template file to upload
     * @param userId Current user ID
     * @return Updated report requirement response
     */
    ReportRequirementResponse updateReportRequirement(Long requirementId, UpdateReportRequirementRequest request, MultipartFile file, Long userId) throws AppException;


    /**
     * Create a report with file upload (draft for team officer, can submit for club president)
     * If autoSubmit is true (or null/default) and user is club president, the report will be automatically submitted.
     * If autoSubmit is false and user is club president, the report will be created as draft.
     * Team officer can only create draft reports regardless of autoSubmit flag.
     * @param request Create request containing report details and optional autoSubmit flag
     * @param file Optional file to upload (will be uploaded to Cloudinary and fileUrl will be set automatically)
     * @param userId Current user ID
     * @return Created report detail response
     */
    ReportDetailResponse createReport(CreateReportRequest request, MultipartFile file, Long userId) throws  AppException;


    /**
     * Update a draft report with file upload
     * @param reportId Report ID
     * @param request Update request containing report details
     * @param file Optional file to upload (will be uploaded to Cloudinary and fileUrl will be set automatically)
     * @param userId Current user ID
     * @return Updated report detail response
     */
    ReportDetailResponse updateReport(Long reportId, UpdateReportRequest request, MultipartFile file, Long userId) throws  AppException;

    /**
     * Submit a draft report (club president or team officer who is the creator)
     * @param request Submit request containing report ID
     * @param userId Current user ID
     * @return Submitted report detail response
     */
    ReportDetailResponse submitReport(SubmitReportRequest request, Long userId) throws AppException;

    /**
     * Get all reports for a club (club president can see all, team officer can see their own)
     * @param clubId Club ID
     * @param status Report status filter
     * @param semesterId Semester ID filter
     * @param reportType Report type filter
     * @param keyword Keyword for searching
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return List of report list item responses
     */
    PageResponse<ReportListItemResponse> getClubReports(
            Long clubId, ReportStatus status, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId);

    /**
     * Get my draft reports for a club
     * @param clubId Club ID
     * @param status Report status filter
     * @param semesterId Semester ID filter
     * @param reportType Report type filter
     * @param keyword Keyword for searching
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return List of draft report list item responses
     */
    PageResponse<ReportListItemResponse> getMyReports(
            Long clubId, ReportStatus status, Long semesterId, ReportType reportType,
            String keyword, Pageable pageable, Long userId);

    /**
     * Get all report requirements with filters and pagination (for staff only)
     * @param reportType Report type filter
     * @param clubId Club ID filter
     * @param keyword Keyword for searching
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return Page response containing list of report requirements
     */
    PageResponse<ReportRequirementResponse> getAllReportRequirements(
            ReportType reportType, Long clubId, String keyword, Pageable pageable, Long userId);

    /**
     * Get a single report requirement by ID (for staff only)
     * @param requirementId Submission report requirement ID
     * @param userId Current user ID
     * @return Report requirement response
     */
    ReportRequirementResponse getReportRequirementById(Long requirementId, Long userId);

    /**
     * Get list of clubs that need to submit reports for a specific report requirement (for staff only)
     * @param requirementId Submission report requirement ID
     * @param keyword Keyword for searching club name or code
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return Page response containing list of club requirement info with club details and status
     */
    PageResponse<ReportRequirementResponse.ClubRequirementInfo> getClubsByReportRequirement(
            Long requirementId, String keyword, Pageable pageable, Long userId);

    /**
     * Get report of a specific club for a specific report requirement (for staff only)
     * @param requirementId Submission report requirement ID
     * @param clubId Club ID
     * @param userId Current user ID
     * @return Report detail response if exists, null otherwise
     */
    ReportDetailResponse getClubReportByRequirement(Long requirementId, Long clubId, Long userId);


    /**
     * Get all report requirements for a club with filters and pagination (for CLUB_OFFICER or TEAM_OFFICER)
     * @param clubId Club ID
     * @param status Status filter (OVERDUE, UNSUBMITTED, DRAFT, PENDING_CLUB, etc.)
     * @param semesterId Semester ID filter
     * @param keyword Keyword for searching
     * @param teamId Team ID filter
     * @param pageable Pageable object for pagination and sorting
     * @param userId Current user ID
     * @return Page response containing list of report requirement responses assigned to the club
     */
    PageResponse<OfficerReportRequirementResponse> getClubReportRequirementsForOfficerWithFilters(
            Long clubId, String status, Long semesterId, String keyword, Long teamId,
            Pageable pageable, Long userId);

    /**
     * Get report of a specific club for a specific report requirement (for CLUB_OFFICER or TEAM_OFFICER)
     * @param requirementId Submission report requirement ID
     * @param clubId Club ID
     * @param userId Current user ID
     * @return Report detail response if exists, null otherwise
     */
    ReportDetailResponse getClubReportByRequirementForOfficer(Long requirementId, Long clubId, Long userId);

    /**
     * Delete a draft report (only creator or team officer can delete their own draft)
     * @param reportId Report ID
     * @param userId Current user ID
     */
    void deleteReport(Long reportId, Long userId) throws AppException;

    /**
     * Review (approve/reject) a report at club level (for club president only)
     * Approve: PENDING_CLUB -> PENDING_UNIVERSITY
     * Reject: PENDING_CLUB -> REJECTED_CLUB
     * @param request Review request containing report ID, status, and optional feedback
     * @param userId Current user ID
     * @return Updated report detail response
     */
    ReportDetailResponse reviewReportByClub(ReportReviewRequest request, Long userId) throws AppException;

    /**
     * Get report detail by report ID for club officers (CLUB_OFFICER or TEAM_OFFICER)
     * @param reportId Report ID
     * @param clubId Club ID
     * @param userId Current user ID
     * @return Report detail response
     */
    ReportDetailResponse getClubReportDetail(Long reportId, Long clubId, Long userId);

    /**
     * Assign a team to a report requirement (for CLUB_OFFICER only)
     * @param clubReportRequirementId Club Report Requirement ID
     * @param teamId Team ID to assign
     * @param clubId Club ID
     * @param userId Current user ID
     * @return Updated report requirement response
     */
    ReportRequirementResponse assignTeamToReportRequirement(Long clubReportRequirementId, Long teamId, Long clubId, Long userId) throws AppException;
}

