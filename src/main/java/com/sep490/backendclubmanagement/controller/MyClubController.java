package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.response.MemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamDTO;
import com.sep490.backendclubmanagement.service.club.club.MyClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-club")
@RequiredArgsConstructor
public class MyClubController {

    private final MyClubService myClubService;

    /**
     * API để người quản lý CLB lấy danh sách thành viên.
     * @return Danh sách thành viên trong CLB của người dùng.
     */
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberDTO>>> getMyClubMembers() {
        List<MemberDTO> members = myClubService.getMembersOfCurrentUserClub();
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    /**
     * API để thành viên CLB lấy danh sách các ban/đội.
     * @return Danh sách các ban/đội trong (các) CLB mà người dùng tham gia.
     */
    @GetMapping("/teams")
    public ResponseEntity<ApiResponse<List<TeamDTO>>> getMyClubTeams() {
        List<TeamDTO> teams = myClubService.getTeamsOfCurrentUserClubs();
        return ResponseEntity.ok(ApiResponse.success(teams));
    }
}
