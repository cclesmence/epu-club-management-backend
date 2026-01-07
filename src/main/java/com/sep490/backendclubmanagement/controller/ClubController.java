package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.UpdateClubInfoRequest;
import com.sep490.backendclubmanagement.dto.response.ClubDetailData;
import com.sep490.backendclubmanagement.dto.response.ClubDto;
import com.sep490.backendclubmanagement.dto.response.TeamDTO;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.club.club.ClubServiceInterface;
import com.sep490.backendclubmanagement.service.team.TeamService;
import com.sep490.backendclubmanagement.dto.response.TeamResponse;
import com.sep490.backendclubmanagement.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clubInfo")
@RequiredArgsConstructor
public class ClubController {

    private final ClubServiceInterface clubService;
    private final TeamService teamService;

    /**
     * Get club detail by ID
     * @param id Club ID
     * @return Club detail data
     */
    @GetMapping("/{id}")
    public ApiResponse<ClubDetailData> getClubDetail(@PathVariable Long id) throws AppException {
        ClubDetailData data = clubService.getClubDetail(id);
        return ApiResponse.success(data);
    }

    /**
     * Get club detail by club code
     * @param clubCode Club code
     * @return Club detail data
     */
    @GetMapping("/code/{clubCode}")
    public ApiResponse<ClubDetailData> getClubDetailByCode(@PathVariable String clubCode) throws AppException {
        ClubDetailData data = clubService.getClubDetailByCode(clubCode);
        return ApiResponse.success(data);
    }

    /**
     * Get all clubs (id and name only)
     * @return List of clubs with id and clubName
     */
    @GetMapping
    public ApiResponse<List<ClubDto>> getAllClubs() {
        List<ClubDto> clubs = clubService.getAllClubs();
        return ApiResponse.success(clubs);
    }

    /**
     * Get club information for club members to view
     * Only accessible by active members of the club or ADMIN/STAFF
     * @param id Club ID
     * @return Club detail data
     */
    @PreAuthorize("@clubSecurity.isMemberOfClub(#id)")
    @GetMapping("/{id}/club-info")
    public ApiResponse<ClubDetailData> getClubInfo(
            @PathVariable Long id) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubDetailData data = clubService.getClubInfo(id, userId);
        return ApiResponse.success(data);
    }

    /**
     * Only club officers can update club information
     * @param id Club ID
     * @param request Update request with club information
     * @param logoFile Logo file (optional)
     * @param bannerFile Banner file (optional)
     * @return Updated club detail data
     */
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#id)")
    @PutMapping(value = "/{id}/officer-update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ClubDetailData> updateClubInfo(
            @PathVariable Long id,
            @Valid @RequestPart("request") UpdateClubInfoRequest request,
            @RequestPart(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestPart(value = "bannerFile", required = false) MultipartFile bannerFile) throws AppException {
        Long userId = SecurityUtils.getCurrentUserId();
        ClubDetailData data = clubService.updateClubInfo(id, request, userId, logoFile, bannerFile);
        return ApiResponse.success(data);
    }

    /**
     * Get teams of a club as TeamDTO
     * Returns teamId, teamName, description, clubId and clubName.
     */
    @GetMapping("/{id}/teams/dto")
    public ApiResponse<List<TeamDTO>> getClubTeamsAsDto(@PathVariable Long id) throws AppException {
        // Fetch teams via TeamService
        List<TeamResponse> teams = teamService.getTeamsByClubId(id);

        // Fetch club detail to get club name (validates existence)
        ClubDetailData clubDetail = clubService.getClubDetail(id);
        String clubName = clubDetail != null ? clubDetail.getClubName() : null;

        List<TeamDTO> result = teams.stream()
                .map(t -> new TeamDTO(
                        t.getId(),
                        t.getTeamName(),
                        t.getDescription(),
                        id,
                        clubName
                ))
                .collect(Collectors.toList());

        return ApiResponse.success(result);
    }
}
