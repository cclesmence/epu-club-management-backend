package com.sep490.backendclubmanagement.controller;

import com.sep490.backendclubmanagement.dto.ApiResponse;
import com.sep490.backendclubmanagement.dto.request.RemoveMemberRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateMemberRoleRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateMemberStatusRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateMemberTeamRequest;
import com.sep490.backendclubmanagement.dto.response.ImportMembersResponse;
import com.sep490.backendclubmanagement.dto.response.MemberResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.SimpleMemberResponse;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import com.sep490.backendclubmanagement.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // Consolidated endpoint for all member filtering needs
    @GetMapping("/{clubId}/members")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<MemberResponse>> getMembers(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "ACTIVE") String status, // "ACTIVE" or "LEFT"
            @RequestParam(required = false) Long semesterId, // Filter by semester
            @RequestParam(required = false) Long roleId, // Filter by club role
            @RequestParam(defaultValue = "true") Boolean isActive, // Filter by role membership active status (default: true)
            @RequestParam(required = false) String searchTerm, // Search by name or student code
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        ClubMemberShipStatus memberStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                memberStatus = ClubMemberShipStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status, will be treated as null (no filter)
            }
        }

        PageResponse<MemberResponse> result = memberService.getMembersWithFilters(
                clubId, memberStatus, semesterId, roleId, isActive, searchTerm, pageable);

        return ApiResponse.success(result);
    }

    // Dedicated endpoint for left members
    @GetMapping("/{clubId}/members/left")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<PageResponse<MemberResponse>> getLeftMembers(
            @PathVariable Long clubId,
            @RequestParam(required = false) String searchTerm, // Search by name or student code
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        PageResponse<MemberResponse> result = memberService.getLeftMembers(
                clubId, searchTerm, pageable);

        return ApiResponse.success(result);
    }

    // Get all active members for selection (e.g., fee assignment)
    @GetMapping("/{clubId}/members/all-active")
    @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
    public ApiResponse<List<SimpleMemberResponse>> getAllActiveMembers(
            @PathVariable Long clubId) {

        List<SimpleMemberResponse> result = memberService.getAllActiveMembersForSelection(clubId);

        return ApiResponse.success(result);
    }

    // Update member role
    @PutMapping("/{clubId}/members/{userId}/role")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    public ApiResponse<String> updateMemberRole(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberRoleRequest request,
            @RequestParam Long currentUserId) throws AppException {
        memberService.updateMemberRole(clubId, userId, request.getRoleId(), request.getSemesterId(), currentUserId);
        return ApiResponse.success("Role updated successfully");
    }

    // Update member team
    @PutMapping("/{clubId}/members/{userId}/team")
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    public ApiResponse<String> updateMemberTeam(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberTeamRequest request) throws AppException {
        memberService.updateMemberTeam(clubId, userId, request.getTeamId(), request.getSemesterId());
        return ApiResponse.success("Team updated successfully");
    }

    // Update member status (active/inactive)
    @PutMapping("/{clubId}/members/{userId}/status")
    @PreAuthorize("@clubSecurity.isTeamOfficerOrClubOfficerOrTreasurerInClub(#clubId)")
    public ApiResponse<String> updateMemberStatus(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            @RequestBody UpdateMemberStatusRequest request) {
        memberService.updateMemberActiveStatus(clubId, userId, request.getIsActive(), request.getSemesterId());
        return ApiResponse.success("Status updated successfully");
    }

    // Remove member from club
    @DeleteMapping("/{clubId}/members/{userId}")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    public ApiResponse<String> removeMember(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            @RequestBody(required = false) RemoveMemberRequest request) {
        memberService.removeMemberFromClub(clubId, userId, request != null ? request.getReason() : null);
        return ApiResponse.success("Member removed successfully");
    }

    // Import members from Excel with history across all semesters
    @PostMapping("/{clubId}/members/import")
    @PreAuthorize("@clubSecurity.isClubOfficerInClub(#clubId)")
    public ApiResponse<ImportMembersResponse> importMembersFromExcel(
            @PathVariable Long clubId,
            @RequestParam("file") MultipartFile file,
            @RequestParam Long currentUserId) {
        try {
            ImportMembersResponse result = memberService.importMembersFromExcel(clubId, file, currentUserId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error(400, "Import failed: " + e.getMessage());
        }
    }
}
