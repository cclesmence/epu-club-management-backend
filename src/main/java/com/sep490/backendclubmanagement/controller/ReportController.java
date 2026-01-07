package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.AssignTeamToReportRequirementRequest;
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
import com.sep490.backendclubmanagement.service.report.ReportServiceInterface;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportServiceInterface reportService;

    /**
     * Get all reports with filters and pagination (for staff only)
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/filter")
    public ApiResponse<PageResponse<ReportListItemResponse>> getAllReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate,desc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ReportListItemResponse> data = reportService.getAllReports(
                status, clubId, semesterId, reportType, keyword, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get report detail by ID (for staff only)
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/{id}")
    public ApiResponse<ReportDetailResponse> getReportDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.getReportDetail(id, userId);
        return ApiResponse.success(data);
    }

    /**
     * Review (approve/reject) a report (for staff only)
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PostMapping("/staff/review")
    public ApiResponse<Void> reviewReport(@RequestBody @Valid ReportReviewRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        reportService.reviewReport(request, userId);
        return ApiResponse.success();
    }

    /**
     * Get all report requirements with filters and pagination (for staff only)
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/requirements/filter")
    public ApiResponse<PageResponse<ReportRequirementResponse>> getAllReportRequirements(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ReportRequirementResponse> data = reportService.getAllReportRequirements(
                reportType, clubId, keyword, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get a single report requirement by ID (for staff only)
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/requirements/{requirementId}")
    public ApiResponse<ReportRequirementResponse> getReportRequirementById(
            @PathVariable Long requirementId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportRequirementResponse data = reportService.getReportRequirementById(requirementId, userId);
        return ApiResponse.success(data);
    }

    /**
     * Create report requirement for multiple clubs (for staff only)
     * Multipart/form-data endpoint
     * File upload is optional. If file is provided, it will be uploaded to Cloudinary.
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PostMapping(value = "/staff/requirements", consumes = "multipart/form-data")
    public ApiResponse<ReportRequirementResponse> createReportRequirement(
            @RequestPart("request") @Valid CreateReportRequirementRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportRequirementResponse data = reportService.createReportRequirement(request, file, userId);
        return ApiResponse.success(data);
    }

    /**
     * Update report requirement basic information (for staff only)
     * Multipart/form-data endpoint
     * File upload is optional. If file is provided, it will be uploaded to Cloudinary.
     * Updates title, description, dueDate, and templateUrl/file.
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PutMapping(value = "/staff/requirements/{requirementId}", consumes = "multipart/form-data")
    public ApiResponse<ReportRequirementResponse> updateReportRequirement(
            @PathVariable Long requirementId,
            @RequestPart("request") @Valid UpdateReportRequirementRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportRequirementResponse data = reportService.updateReportRequirement(requirementId, request, file, userId);
        return ApiResponse.success(data);
    }

    /**
     * Create a report (draft for team officer, can submit for club president)
     * Multipart/form-data endpoint
     * File upload is optional. If file is provided, it will be uploaded to Cloudinary.
     * If autoSubmit is true (or null/default) and user is club president, the report will be automatically submitted.
     * If autoSubmit is false and user is club president, the report will be created as draft.
     * Team officer can only create draft reports regardless of autoSubmit flag.
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#request.clubId)")
    @PostMapping(value = "/club", consumes = "multipart/form-data")
    public ApiResponse<ReportDetailResponse> createReport(
            @RequestPart("request") @Valid CreateReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.createReport(request, file, userId);
        return ApiResponse.success(data);
    }

    /**
     * Update a draft report
     * Multipart/form-data endpoint
     * File upload is optional. If file is provided, it will be uploaded to Cloudinary.
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerForReport(#reportId)")
    @PutMapping(value = "/club/{reportId}", consumes = "multipart/form-data")
    public ApiResponse<ReportDetailResponse> updateReport(
            @PathVariable Long reportId,
            @RequestPart("request") @Valid UpdateReportRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws  AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.updateReport(reportId, request, file, userId);
        return ApiResponse.success(data);
    }

    /**
     * Submit a draft report (club president or team officer who is the creator)
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerForReport(#request.reportId)")
    @PostMapping("/club/submit")
    public ApiResponse<ReportDetailResponse> submitReport(
            @RequestBody @Valid SubmitReportRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.submitReport(request, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get all reports for a club (club president can see all, team officer can see their own)
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    @GetMapping("/club/{clubId}")
    public ApiResponse<PageResponse<ReportListItemResponse>> getClubReports(
            @PathVariable Long clubId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate,desc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ReportListItemResponse> data = reportService.getClubReports(
                clubId, status, semesterId, reportType, keyword, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get my reports for a club
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    @GetMapping("/club/{clubId}/my-reports")
    public ApiResponse<PageResponse<ReportListItemResponse>> getMyReports(
            @PathVariable Long clubId,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate,desc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ReportListItemResponse> data = reportService.getMyReports(
                clubId, status, semesterId, reportType, keyword, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get all report requirements for a club with filters and pagination (for CLUB_OFFICER or TEAM_OFFICER)
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    @GetMapping("/club/{clubId}/requirements/officer/filter")
    public ApiResponse<PageResponse<OfficerReportRequirementResponse>> getClubReportRequirementsForOfficerWithFilters(
            @PathVariable Long clubId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teamId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "deadline,desc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<OfficerReportRequirementResponse> data = reportService.getClubReportRequirementsForOfficerWithFilters(
                clubId, status, semesterId, keyword, teamId, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get report of a specific club for a specific report requirement (for CLUB_OFFICER or TEAM_OFFICER)
     * Returns null if club hasn't submitted report yet
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    @GetMapping("/club/{clubId}/requirements/{requirementId}/report")
    public ApiResponse<ReportDetailResponse> getClubReportByRequirementForOfficer(
            @PathVariable Long requirementId,
            @PathVariable Long clubId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.getClubReportByRequirementForOfficer(requirementId, clubId, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get report detail by report ID for club officers (CLUB_OFFICER or TEAM_OFFICER)
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    @GetMapping("/club/{clubId}/reports/{reportId}")
    public ApiResponse<ReportDetailResponse> getClubReportDetail(
            @PathVariable Long clubId,
            @PathVariable Long reportId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.getClubReportDetail(reportId, clubId, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get list of clubs that need to submit reports for a specific report requirement (for staff only)
     * Supports pagination and search by club name or code
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/requirements/{requirementId}/clubs")
    public ApiResponse<PageResponse<ReportRequirementResponse.ClubRequirementInfo>> getClubsByReportRequirement(
            @PathVariable Long requirementId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ReportRequirementResponse.ClubRequirementInfo> data = reportService.getClubsByReportRequirement(
                requirementId, keyword, pageable, userId);
        return ApiResponse.success(data);
    }

    /**
     * Get report of a specific club for a specific report requirement (for staff only)
     * Returns null if club hasn't submitted report yet
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/staff/requirements/{requirementId}/clubs/{clubId}/report")
    public ApiResponse<ReportDetailResponse> getClubReportByRequirement(
            @PathVariable Long requirementId,
            @PathVariable Long clubId
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.getClubReportByRequirement(requirementId, clubId, userId);
        return ApiResponse.success(data);
    }

    /**
     * Delete a draft report (only creator can delete their own draft)
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerForReport(#reportId)")
    @DeleteMapping("/club/{reportId}")
    public ApiResponse<Void> deleteReport(@PathVariable Long reportId) throws  AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        reportService.deleteReport(reportId, userId);
        return ApiResponse.success();
    }

    /**
     * Review (approve/reject) a report at club level (for club president only)
     * Approve: PENDING_CLUB -> PENDING_UNIVERSITY
     * Reject: PENDING_CLUB -> REJECTED_CLUB
     */
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerForReport(#request.reportId)")
    @PostMapping(value = "/club/review", consumes = "application/json")
    public ApiResponse<ReportDetailResponse> reviewReportByClub(
            @RequestBody @Valid ReportReviewRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportDetailResponse data = reportService.reviewReportByClub(request, userId);
        return ApiResponse.success(data);
    }

    /**
     * Assign a team to a report requirement (for CLUB_OFFICER only)
     */
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    @PostMapping("/club/{clubId}/requirements/assign-team")
    public ApiResponse<ReportRequirementResponse> assignTeamToReportRequirement(
            @PathVariable Long clubId,
            @RequestBody @Valid AssignTeamToReportRequirementRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportRequirementResponse data = reportService.assignTeamToReportRequirement(
                request.getClubReportRequirementId(),
                request.getTeamId(),
                clubId,
                userId
        );
        return ApiResponse.success(data);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String prop = parts.length > 0 ? parts[0] : "createdAt";
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }
}
