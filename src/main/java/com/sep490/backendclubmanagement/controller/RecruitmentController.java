package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.*;
import com.sep490.backendclubmanagement.dto.response.*;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentApplicationStatus;
import com.sep490.backendclubmanagement.entity.recruitment.RecruitmentStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.security.ClubSecurity;
import com.sep490.backendclubmanagement.service.recruitment.RecruitmentServiceInterface;
import com.sep490.backendclubmanagement.service.user.UserService;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
public class RecruitmentController {

    private final RecruitmentServiceInterface recruitmentService;
    private final UserService userService;
    private final ClubSecurity clubSecurity;

    @GetMapping("/clubs/{clubId}")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    public ApiResponse<PagedResponse<RecruitmentData>> listRecruitments(
            @PathVariable Long clubId,
            @RequestParam(required = false) RecruitmentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        // Convert from 1-based to 0-based pagination for Spring Data
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PagedResponse<RecruitmentData> data = recruitmentService.listRecruitments(userId,clubId, status, keyword, pageable);
        return ApiResponse.success(data);
    }

    @GetMapping("/clubs/{clubId}/open")
    public ApiResponse<PagedResponse<RecruitmentData>> listOpenRecruitments (
            @PathVariable Long clubId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort
    ) throws AppException {
        // Convert from 1-based to 0-based pagination for Spring Data
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PagedResponse<RecruitmentData> data = recruitmentService.listRecruitmentsForGuest(clubId, RecruitmentStatus.OPEN, pageable);
        return ApiResponse.success(data);
    }

    /**
     * Get recruitment detail by ID
     * Public endpoint - accessible by anyone to view recruitment form
     */
    @GetMapping("/{id}")
    public ApiResponse<RecruitmentData> getRecruitment(@PathVariable Long id) throws AppException {
        RecruitmentData data = recruitmentService.getRecruitment(id);
        return ApiResponse.success(data);
    }

    @PostMapping("/clubs/{clubId}")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    public ApiResponse<RecruitmentData> createRecruitment(
            @PathVariable Long clubId,
            @RequestBody RecruitmentCreateRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        RecruitmentData data = recruitmentService.createRecruitment(userId, clubId, request);
        return ApiResponse.success(data);
    }

    /**
     * Update recruitment
     * Must be club officer of the recruitment's club
     */
    @PutMapping("/{id}")
    @PreAuthorize("@clubSecurity.isClubOfficerForRecruitment(#id)")
    public ApiResponse<RecruitmentData> updateRecruitment(
            @PathVariable Long id,
            @RequestBody RecruitmentUpdateRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();

        RecruitmentData data = recruitmentService.updateRecruitment(userId, id, request);
        return ApiResponse.success(data);
    }

    /**
     * Change recruitment status
     * Must be club officer of the recruitment's club
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("@clubSecurity.isClubOfficerForRecruitment(#id)")
    public ApiResponse<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam RecruitmentStatus status
    ) throws AppException {

        Long userId = SecurityUtils.getCurrentUserId();
        recruitmentService.changeRecruitmentStatus(userId, id, status);
        return ApiResponse.success();
    }


    /**
     * Submit application to recruitment
     * Any authenticated user can submit application
     */
    @PostMapping(path = "/applications/submit", consumes = "multipart/form-data")
    public ApiResponse<RecruitmentApplicationData> submit(
            @RequestPart("request") ApplicationSubmitRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        RecruitmentApplicationData data = recruitmentService.submitApplication(userId, request, file);
        return ApiResponse.success(data);
    }

    /**
     * Review application (approve/reject)
     * Must be club officer of the application's recruitment's club
     * Note: Authorization is checked inside method because applicationId is in request body
     */
    @PostMapping("/applications/review")
    @PreAuthorize("@clubSecurity.isClubOfficerForApplication(#request.applicationId)")
    public ApiResponse<RecruitmentApplicationData> review(
            @RequestBody ApplicationReviewRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        RecruitmentApplicationData data = recruitmentService.reviewApplication(userId, request);
        return ApiResponse.success(data);
    }

    /**
     * Update interview schedule
     * Must be club officer of the application's recruitment's club
     * Can only update before interview time
     */
    @PutMapping("/applications/interview")
    @PreAuthorize("@clubSecurity.isClubOfficerForApplication(#request.applicationId)")
    public ApiResponse<RecruitmentApplicationData> updateInterviewSchedule(
            @RequestBody InterviewUpdateRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();

        RecruitmentApplicationData data = recruitmentService.updateInterviewSchedule(userId, request);
        return ApiResponse.success(data);
    }

    /**
     * List applications for a recruitment
     * Must be club officer - detailed permission check done in service layer
     */
    @GetMapping("/{recruitmentId}/applications")
    @PreAuthorize("@clubSecurity.isClubOfficerForRecruitment(#recruitmentId)")
    public ApiResponse<PagedResponse<RecruitmentApplicationListData>> listApplications(
            @PathVariable Long recruitmentId,
            @RequestParam(required = false) RecruitmentApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate,desc") String sort
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // Convert from 1-based to 0-based pagination for Spring Data
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PagedResponse<RecruitmentApplicationListData> data = recruitmentService.listApplications(userId, recruitmentId, status, keyword, pageable);
        return ApiResponse.success(data);
    }

    /**
     * Get application by ID
     * Must be club officer of the application's recruitment's club
     */
    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("@clubSecurity.isClubOfficerForApplication(#applicationId)")
    public ApiResponse<RecruitmentApplicationData> getApplication(
            @PathVariable Long applicationId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        RecruitmentApplicationData data = recruitmentService.getApplication(userId, applicationId);
        return ApiResponse.success(data);
    }

    /**
     * Get applications for current user
     * Any authenticated user can view their own applications
     */
    @GetMapping("/myApplications")
    public ApiResponse<PagedResponse<RecruitmentApplicationListData>> getMyApplications(
            @RequestParam(required = false) RecruitmentApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedDate,desc") String sort
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        // Convert from 1-based to 0-based pagination for Spring Data
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PagedResponse<RecruitmentApplicationListData> data = recruitmentService.listMyApplications(userId, status, keyword, pageable);
        return ApiResponse.success(data);
    }

    /**
     * Get a specific application that the current user submitted
     * Any authenticated user can view their own application detail
     */
    @GetMapping("/myApplications/{applicationId}")
    public ApiResponse<RecruitmentApplicationData> getMyApplication(
            @PathVariable Long applicationId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        RecruitmentApplicationData data = recruitmentService.getMyApplication(userId, applicationId);
        return ApiResponse.success(data);
    }

    /**
     * Check if current user has already applied for a recruitment
     * Returns application status if exists
     * Any authenticated user can check their own application status
     */
    @GetMapping("/{recruitmentId}/check-application")
    public ApiResponse<ApplicationStatusCheckData> checkApplicationStatus(
            @PathVariable Long recruitmentId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        
        ApplicationStatusCheckData data = recruitmentService.checkApplicationStatus(userId, recruitmentId);
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


