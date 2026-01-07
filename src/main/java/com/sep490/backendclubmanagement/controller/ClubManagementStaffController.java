package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateClubRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRequest;
import com.sep490.backendclubmanagement.dto.response.ClubManagementResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.club.club.ClubServiceInterface;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Staff to manage clubs
 */
@RestController
@RequestMapping("/api/staff/clubs")
@RequiredArgsConstructor
public class ClubManagementStaffController {

    private final ClubServiceInterface clubService;

    /**
     * Get clubs with filter, search and pagination
     * GET /api/staff/clubs
     * @param keyword Search by club name or club code
     * @param campusId Filter by campus ID
     * @param categoryId Filter by category ID
     * @param status Filter by status
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param sort Sort parameter (default: "createdAt,desc")
     * @return PageResponse of clubs
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping
    public ApiResponse<PageResponse<ClubManagementResponse>> getClubs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page - 1, size, parseSort(sort));
        PageResponse<ClubManagementResponse> response = clubService.getClubsByFilter(
                keyword, campusId, categoryId, status, pageable, userId);
        return ApiResponse.success(response);
    }

    /**
     * Get club detail (for editing)
     * GET /api/staff/clubs/{clubId}
     * @param clubId Club ID
     * @return Club detail
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @GetMapping("/{clubId}")
    public ApiResponse<ClubManagementResponse> getClubDetail(
            @PathVariable Long clubId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubManagementResponse clubDetail = clubService.getClubForManagement(clubId, userId);
        return ApiResponse.success(clubDetail);
    }

    /**
     * Create new club
     * POST /api/staff/clubs
     * @param request Create club request (including presidentEmail)
     * @return Created club
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PostMapping
    public ApiResponse<ClubManagementResponse> createClub(
            @Valid @RequestBody CreateClubRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubManagementResponse response = clubService.createClub(request, userId);
        return ApiResponse.success(response);
    }

    /**
     * Update club
     * PUT /api/staff/clubs/{clubId}
     * @param clubId Club ID
     * @param request Update club request
     * @return Updated club
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PutMapping("/{clubId}")
    public ApiResponse<ClubManagementResponse> updateClub(
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubRequest request
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubManagementResponse response = clubService.updateClub(clubId, request, userId);
        return ApiResponse.success(response);
    }

    /**
     * Change club status to UNACTIVE
     * PATCH /api/staff/clubs/{clubId}/deactivate
     * @param clubId Club ID
     * @return Success message
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PatchMapping("/{clubId}/deactivate")
    public ApiResponse<Void> deactivateClub(
            @PathVariable Long clubId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        clubService.deactivateClub(clubId, userId);
        return ApiResponse.success();
    }

    /**
     * Change club status to ACTIVE
     * PATCH /api/staff/clubs/{clubId}/activate
     * @param clubId Club ID
     * @return Success message
     */
    @PreAuthorize("@clubSecurity.isStaff()")
    @PatchMapping("/{clubId}/activate")
    public ApiResponse<Void> activateClub(
            @PathVariable Long clubId
    ) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        clubService.activateClub(clubId, userId);
        return ApiResponse.success();
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",");
        String prop = parts.length > 0 ? parts[0] : "createdAt";
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, prop);
    }
}

