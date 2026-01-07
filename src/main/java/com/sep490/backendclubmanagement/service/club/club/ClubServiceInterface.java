package com.sep490.backendclubmanagement.service.club.club;

import com.sep490.backendclubmanagement.dto.request.CreateClubRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubInfoRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRequest;
import com.sep490.backendclubmanagement.dto.response.ClubDetailData;
import com.sep490.backendclubmanagement.dto.response.ClubDto;
import com.sep490.backendclubmanagement.dto.response.ClubManagementResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Interface for Club Service
 */
public interface ClubServiceInterface {

    /**
     * Get club detail by ID
     * @param clubId Club ID
     * @return ClubDetailData
     * @throws AppException if club not found
     */
    ClubDetailData getClubDetail(Long clubId) throws AppException;

    /**
     * Get club detail by club code
     * @param clubCode Club code
     * @return ClubDetailData
     * @throws AppException if club not found
     */
    ClubDetailData getClubDetailByCode(String clubCode) throws AppException;

    /**
     * Get all clubs (id and name only)
     * @return List of ClubDto with id and clubName
     */
    List<ClubDto> getAllClubs();

    /**
     * Get clubs with filter, search, and pagination (Staff only)
     * @param keyword Search by club name or club code
     * @param campusId Filter by campus ID
     * @param categoryId Filter by category ID
     * @param status Filter by status
     * @param pageable Pageable object for pagination and sorting
     * @param staffId Staff user ID for permission check
     * @return PageResponse of ClubManagementResponse
     * @throws AppException if user doesn't have STAFF role
     */
    PageResponse<ClubManagementResponse> getClubsByFilter(
            String keyword, Long campusId, Long categoryId, String status,
            Pageable pageable, Long staffId) throws AppException;

    /**
     * Create new club (Staff only)
     * @param request Create club request
     * @param staffId Staff user ID for permission check
     * @return Created club response
     * @throws AppException if validation fails
     */
    ClubManagementResponse createClub(CreateClubRequest request, Long staffId) throws AppException;

    /**
     * Update club (Staff only)
     * @param clubId Club ID
     * @param request Update club request
     * @param staffId Staff user ID for permission check
     * @return Updated club response
     * @throws AppException if club not found or validation fails
     */
    ClubManagementResponse updateClub(Long clubId, UpdateClubRequest request, Long staffId) throws AppException;

    /**
     * Deactivate club (change status to UNACTIVE) (Staff only)
     * @param clubId Club ID
     * @param staffId Staff user ID for permission check
     * @throws AppException if club not found
     */
    void deactivateClub(Long clubId, Long staffId) throws AppException;

    /**
     * Activate club (change status to ACTIVE) (Staff only)
     * @param clubId Club ID
     * @param staffId Staff user ID for permission check
     * @throws AppException if club not found
     */
    void activateClub(Long clubId, Long staffId) throws AppException;

    /**
     * Get club for management detail (Staff only)
     * @param clubId Club ID
     * @param staffId Staff user ID for permission check
     * @return ClubManagementResponse
     * @throws AppException if club not found
     */
    ClubManagementResponse getClubForManagement(Long clubId, Long staffId) throws AppException;

    /**
     * Get club information for club members to view/edit
     * @param clubId Club ID
     * @param userId User ID (must be an active member of the club)
     * @return ClubDetailData with full information
     * @throws AppException if club not found or user is not a member of the club
     */
    ClubDetailData getClubInfo(Long clubId, Long userId) throws AppException;

    /**
     * Update club information (Club Officer only)
     * @param clubId Club ID
     * @param request Update request with club information
     * @param userId User ID (must be club officer)
     * @param logoFile Logo file (optional)
     * @param bannerFile Banner file (optional)
     * @return Updated ClubDetailData
     * @throws AppException if club not found, validation fails, or user is not club officer
     */
    ClubDetailData updateClubInfo(Long clubId, UpdateClubInfoRequest request, Long userId, MultipartFile logoFile, MultipartFile bannerFile) throws AppException;
}


