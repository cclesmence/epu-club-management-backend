package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.MyTeamRoleResponse;
import com.sep490.backendclubmanagement.service.team.TeamRoleService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}/teams")
public class TeamRoleController {

    private final TeamRoleService teamRoleService;
    private final UserService userService;

    @GetMapping("/{teamId}/my-role")
    public ApiResponse<MyTeamRoleResponse> getMyRole(
            @AuthenticationPrincipal User principal,
            @PathVariable Long clubId,
            @PathVariable Long teamId
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        return ApiResponse.success(teamRoleService.getMyRole(me, clubId, teamId));
    }
}
