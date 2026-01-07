package com.sep490.backendclubmanagement.service.team;

import com.sep490.backendclubmanagement.dto.request.CreateTeamRequest;
import com.sep490.backendclubmanagement.dto.request.UpdateTeamRequest;
import com.sep490.backendclubmanagement.dto.response.AvailableMemberDTO;
import com.sep490.backendclubmanagement.dto.response.TeamResponse;

import java.util.List;

public interface TeamService {
    List<TeamResponse> getTeamsByClubId(Long clubId);
    TeamResponse createTeam(CreateTeamRequest request);
    List<AvailableMemberDTO> getAvailableMembers(Long clubId);
    TeamResponse updateTeam(Long teamId, UpdateTeamRequest request);
    void deleteTeam(Long teamId);

}
