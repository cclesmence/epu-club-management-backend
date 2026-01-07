package com.sep490.backendclubmanagement.service.club.role;

import com.sep490.backendclubmanagement.dto.request.CreateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateClubRoleRequest;
import com.sep490.backendclubmanagement.dto.response.ClubRoleResponse;
import com.sep490.backendclubmanagement.exception.AppException;

import java.util.List;

public interface ClubRoleService {
    List<ClubRoleResponse> getClubRolesByClubId(Long clubId);

    boolean isClubLeaderOrVice(Long userId, Long clubId);
    boolean isTeamLeader(Long userId, Long teamId);
    ClubRoleResponse createClubRole(Long clubId,
                                    CreateClubRoleRequest request) throws AppException;
    // NEW: edit role
    ClubRoleResponse updateClubRole(Long clubId,
                                    Long roleId,
                                    UpdateClubRoleRequest request) throws AppException;
    void deleteClubRole(Long clubId, Long roleId) throws AppException;

}

