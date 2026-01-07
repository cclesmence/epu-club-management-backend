package com.sep490.backendclubmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamDetailDTO {
    private Long teamId;
    private String teamName;
    private String description;
    private long memberCount;
    private List<TeamMemberDTO> members;
    private List<ActivityDTO> activities;
}