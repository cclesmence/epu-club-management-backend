package com.sep490.backendclubmanagement.service.team;

import com.sep490.backendclubmanagement.dto.response.MyTeamRoleResponse;

public interface TeamRoleService {
    MyTeamRoleResponse getMyRole(Long me, Long clubId, Long teamId);
}
