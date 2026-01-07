package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.service.club.club.ClubAccessService;
import com.sep490.backendclubmanagement.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clubs/{clubId}")
public class ClubAccessController {

    private final ClubAccessService clubAccessService;
    private final UserService userService;

    @GetMapping("/am-i-officer")
    public ApiResponse<Boolean> amIOfficer(
            @AuthenticationPrincipal User principal,
            @PathVariable Long clubId
    ) {
        Long me = userService.getIdByEmail(principal.getUsername());
        boolean ok = clubAccessService.amOfficer(me, clubId);
        return ApiResponse.success(ok);
    }
}