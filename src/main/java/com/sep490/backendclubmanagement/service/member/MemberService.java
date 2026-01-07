package com.sep490.backendclubmanagement.service.member;

import com.sep490.backendclubmanagement.dto.response.ImportMembersResponse;
import com.sep490.backendclubmanagement.dto.response.MemberResponse;
import com.sep490.backendclubmanagement.dto.response.PageResponse;
import com.sep490.backendclubmanagement.dto.response.SimpleMemberResponse;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import com.sep490.backendclubmanagement.exception.AppException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MemberService {
    
    // Consolidated method for all filtering needs
    PageResponse<MemberResponse> getMembersWithFilters(
        Long clubId, 
        ClubMemberShipStatus status, 
        Long semesterId, 
        Long roleId,
        Boolean isActive,
        String searchTerm, 
        Pageable pageable
    );
    
    // Dedicated method for filtering left members
    PageResponse<MemberResponse> getLeftMembers(
        Long clubId,
        String searchTerm,
        Pageable pageable
    );

    // Get all active members for selection (e.g., fee assignment)
    List<SimpleMemberResponse> getAllActiveMembersForSelection(Long clubId);

    void updateMemberRole(Long clubId, Long userId, Long roleId, Long semesterId, Long currentUserId) throws AppException;

    void updateMemberTeam(Long clubId, Long userId, Long teamId, Long semesterId) throws AppException;

    void updateMemberActiveStatus(Long clubId, Long userId, boolean isActive, Long semesterId);

    void removeMemberFromClub(Long clubId, Long userId, String reason);

    ImportMembersResponse importMembersFromExcel(Long clubId, MultipartFile file, Long currentUserId) throws Exception;
}
