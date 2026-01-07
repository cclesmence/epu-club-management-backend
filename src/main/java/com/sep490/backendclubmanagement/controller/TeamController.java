package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.CreateTeamRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateTeamRequest;
import com.sep490.backendclubmanagement.dto.response.AvailableMemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamResponse;
import com.sep490.backendclubmanagement.exception.AccessDeniedException;
import com.sep490.backendclubmanagement.security.RoleGuard;
import com.sep490.backendclubmanagement.service.team.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final RoleGuard guard;


    /**
     * API linh hoạt: Tạo phòng ban (team).
     * - Chỉ tạo team, hoặc
     * - Tạo team và gán Leader / Vice / Members.
     * Hệ thống kiểm tra trùng lặp, ràng buộc học kỳ hiện tại và tư cách thành viên CLB.
     */
    @PostMapping
    public ApiResponse<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        Long currentUserId = guard.getCurrentUserId();

        if (!guard.isClubManager(currentUserId, request.getClubId())) {
            throw new AccessDeniedException("Chỉ Chủ nhiệm hoặc Phó chủ nhiệm CLB mới được phép tạo phòng ban.");
        }

        TeamResponse team = teamService.createTeam(request);
        return ApiResponse.success(team);
    }

    @GetMapping("/clubs/{clubId}/available-members")
    public ApiResponse<List<AvailableMemberDTO>> getAvailableMembers(@PathVariable Long clubId) {
        return ApiResponse.success(teamService.getAvailableMembers(clubId));
    }
    @PatchMapping("/{teamId}")
    public ApiResponse<TeamResponse> updateTeam(
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request
    ) {
        TeamResponse updated = teamService.updateTeam(teamId, request);
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{teamId}")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId) {
        teamService.deleteTeam(teamId);
        return ApiResponse.success(null);
    }

}
